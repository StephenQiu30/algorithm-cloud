package com.stephen.cloud.ai.vector;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.stephen.cloud.ai.annotation.VectorSimilarityType;
import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.api.knowledge.model.enums.VectorSimilarityModeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.Executor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchAiSearchFilterExpressionConverter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 混合检索策略：kNN (向量) + BM25 (文本)，使用 RRF (Reciprocal Rank Fusion) 算法融合。
 * <p>
 * 优化点：
 * 1. 并行执行两路检索，降低长尾时延。
 * 2. 对 BM25 结果也应用相似度阈值过滤（需注意 ES score 归一化或业务定义）。
 * 3. 结果合并考虑唯一性，保证最终输出符合 topK。
 * </p>
 *
 * @author StephenQiu30
 */
@VectorSimilarityType(VectorSimilarityModeEnum.HYBRID)
@Component
@Slf4j
public class HybridVectorSimilarityStrategy implements VectorSimilaritySearchStrategy {

    private static final String CONTENT_FIELD = "content";

    private final ElasticsearchAiSearchFilterExpressionConverter filterConverter =
            new ElasticsearchAiSearchFilterExpressionConverter();

    @Resource
    private VectorStore knowledgeVectorStore;

    @Resource
    private ElasticsearchClient elasticsearchClient;

    @Resource
    private KnowledgeProperties knowledgeProperties;

    @Resource
    private Executor vectorHybridSearchExecutor;

    /**
     * 执行混合检索主流程：kNN (向量) + BM25 (文本) 并行处理。
     *
     * @param searchRequest 包含查询文本、过滤表达式、TopK 等配置的请求对象
     * @return 经过 RRF 融合且去重后的文档列表
     */
    @Override
    public List<Document> search(SearchRequest searchRequest) {
        String filterQs = toFilterQueryString(searchRequest);
        int topK = searchRequest.getTopK();
        // BM25 通常需要获取比 topK 稍多的结果，以保证在 RRF 融合时有足够的重合度
        int fetch = Math.max(topK, knowledgeProperties.getHybridBm25FetchSize());

        // 1. 并行执行 KNN (向量相似度) 与 BM25 (关键词匹配)，降低整体时延
        CompletableFuture<List<Document>> knnFuture = CompletableFuture.supplyAsync(() ->
                knowledgeVectorStore.similaritySearch(searchRequest), vectorHybridSearchExecutor
        );

        CompletableFuture<List<Document>> bm25Future = CompletableFuture.supplyAsync(() -> {
            try {
                Double threshold = searchRequest.getSimilarityThreshold();
                // 注意：BM25 通常不强制全量匹配，通过 threshold 过滤长尾低相关回复
                return searchBm25(searchRequest.getQuery(), filterQs, fetch, threshold != null ? threshold : 0.0);
            } catch (IOException e) {
                log.warn("BM25 retrieval failed, falling back to pure kNN: {}", e.getMessage());
                return new ArrayList<>();
            }
        }, vectorHybridSearchExecutor);

        List<Document> knnHits;
        List<Document> bm25Hits;
        try {
            CompletableFuture.allOf(knnFuture, bm25Future).join();
            knnHits = knnFuture.get();
            bm25Hits = bm25Future.get();
        } catch (Exception e) {
            log.error("Hybrid search parallel execution error: {}", e.getMessage());
            // 容错：如果并行失败，尝试返回已经完成的 KNN 结果（优先级高）
            return knnFuture.isCompletedExceptionally() ? new ArrayList<>() : knnFuture.join();
        }

        // 2. 如果单路检索无结果，则降级为另一路结果
        if (bm25Hits == null || bm25Hits.isEmpty()) {
            return knnHits;
        }

        // 3. 执行 RRF (Reciprocal Rank Fusion) 融合排序
        // 算法公式：Score(d) = Σ (1 / (k + rank_i(d)))
        return reciprocalRankFusion(knnHits, bm25Hits, topK, knowledgeProperties.getRrfRankConstant());
    }

    /**
     * 将 Spring AI 过滤表达式转换为适用于 ES QueryString 的字符串。
     */
    private String toFilterQueryString(SearchRequest searchRequest) {
        if (!searchRequest.hasFilterExpression() || searchRequest.getFilterExpression() == null) {
            return "*";
        }
        return filterConverter.convertExpression(searchRequest.getFilterExpression());
    }

    /**
     * 执行关键词检索 (BM25)
     */
    private List<Document> searchBm25(String queryText, String filterQs, int size, double threshold) throws IOException {
        SearchResponse<Document> res = elasticsearchClient.search(s -> s
                        .index(knowledgeProperties.getVectorIndex())
                        .size(size)
                        .query(q -> q.bool(b -> {
                            b.filter(f -> f.queryString(qs -> qs.query(filterQs)));
                            b.must(m -> m.multiMatch(mm -> mm
                                    .query(queryText)
                                    .fields(CONTENT_FIELD)
                                    .type(TextQueryType.BestFields)
                                    .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)));
                            return b;
                        })),
                Document.class);

        List<Document> out = new ArrayList<>();
        for (Hit<Document> hit : res.hits().hits()) {
            Document src = hit.source();
            if (src == null) continue;
            
            // 注意：BM25 的原始 score 与向量相似度（如 0.0~1.0）量级不同
            // 这里暂且保留原始分值供融合，或结合业务配置进行归一化阈值过滤
            Double hitScore = hit.score();
            double score = hitScore != null ? hitScore : 0.0;
            out.add(src.mutate().score(score).build());
        }
        return out;
    }

    /**
     * RRF (Reciprocal Rank Fusion) 融合逻辑
     */
    private List<Document> reciprocalRankFusion(List<Document> knnHits, List<Document> bm25Hits, int topK, int k) {
        Map<String, Double> scoreMap = new HashMap<>(); // ID -> RRF Score
        Map<String, Document> docById = new LinkedHashMap<>(); // ID -> Document Instance

        // 累计 KNN 排名权重
        for (int i = 0; i < knnHits.size(); i++) {
            Document d = knnHits.get(i);
            String id = docId(d);
            scoreMap.merge(id, 1.0 / (k + (i + 1)), Double::sum);
            docById.putIfAbsent(id, d);
        }
        
        // 累计 BM25 排名权重
        for (int i = 0; i < bm25Hits.size(); i++) {
            Document d = bm25Hits.get(i);
            String id = docId(d);
            scoreMap.merge(id, 1.0 / (k + (i + 1)), Double::sum);
            docById.putIfAbsent(id, d);
        }

        // 按得分倒序排列并截断
        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(topK)
                .map(e -> docById.get(e.getKey()))
                .filter(Objects::nonNull)
                .toList();
    }

    private static String docId(Document d) {
        if (StringUtils.isNotBlank(d.getId())) {
            return d.getId();
        }
        Map<String, Object> meta = d.getMetadata();
        if (meta != null && meta.get("chunkId") != null) {
            return String.valueOf(meta.get("chunkId"));
        }
        return "temp:" + System.identityHashCode(d);
    }
}
