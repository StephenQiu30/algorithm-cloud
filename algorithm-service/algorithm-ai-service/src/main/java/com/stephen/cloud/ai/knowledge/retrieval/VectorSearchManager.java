package com.stephen.cloud.ai.knowledge.retrieval;

import cn.hutool.core.convert.Convert;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.api.knowledge.model.enums.VectorSimilarityModeEnum;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchAiSearchFilterExpressionConverter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 向量检索统一管理器：整合了 KNN 与 Hybrid (RRF 融合) 检索逻辑。
 * <p>
 * 采用单一类实现，降低类爆炸风险，同时保证混合检索的高性能与稳定性。
 * </p>
 *
 * @author StephenQiu30
 */
@Component
@Slf4j
public class VectorSearchManager {

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
     * 核心检索逻辑
     */
    public List<Document> search(SearchRequest searchRequest, VectorSimilarityModeEnum mode) {
        if (mode == VectorSimilarityModeEnum.HYBRID) {
            return searchHybrid(searchRequest);
        }
        return searchKnn(searchRequest);
    }

    /**
     * 执行纯向量 kNN 检索
     */
    private List<Document> searchKnn(SearchRequest searchRequest) {
        return knowledgeVectorStore.similaritySearch(searchRequest);
    }

    /**
     * 执行混合检索 (向量 kNN + 文本 BM25)，使用 RRF 融合。
     */
    private List<Document> searchHybrid(SearchRequest searchRequest) {
        String filterQs = toFilterQueryString(searchRequest);
        int topK = searchRequest.getTopK();
        int fetch = Math.max(topK, knowledgeProperties.getHybridBm25FetchSize());

        // 并行路：kNN 与 BM25
        CompletableFuture<List<Document>> knnFuture = CompletableFuture.supplyAsync(() ->
                searchKnn(searchRequest), vectorHybridSearchExecutor
        );

        CompletableFuture<List<Document>> bm25Future = CompletableFuture.supplyAsync(() -> {
            try {
                Double threshold = searchRequest.getSimilarityThreshold();
                return searchBm25(searchRequest.getQuery(), filterQs, fetch, threshold != null ? threshold : 0.0);
            } catch (IOException e) {
                log.warn("BM25 retrieval failed, falling back to pure kNN: {}", e.getMessage());
                return new ArrayList<>();
            }
        }, vectorHybridSearchExecutor);

        try {
            CompletableFuture.allOf(knnFuture, bm25Future).join();
            List<Document> knnHits = knnFuture.get();
            List<Document> bm25Hits = bm25Future.get();

            if (bm25Hits == null || bm25Hits.isEmpty()) {
                return knnHits;
            }

            return reciprocalRankFusion(knnHits, bm25Hits, topK, knowledgeProperties.getRrfRankConstant());
        } catch (Exception e) {
            log.error("Hybrid search execution error: {}", e.getMessage(), e);
            return knnFuture.isCompletedExceptionally() ? new ArrayList<>() : knnFuture.join();
        }
    }

    /**
     * BM25 文本检索
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
                                    .minimumShouldMatch(knowledgeProperties.getBm25MinimumShouldMatch())));
                            return b;
                        })),
                Document.class);

        List<Document> out = new ArrayList<>();
        for (Hit<Document> hit : res.hits().hits()) {
            Document src = hit.source();
            if (src == null) continue;
            Double hitScore = hit.score();
            double score = hitScore != null ? hitScore : 0.0;
            if (score < threshold) continue;
            out.add(src.mutate().score(score).build());
        }
        return out;
    }

    /**
     * RRF 融合逻辑 (分值 = Σ 1 / (k + rank))
     */
    private List<Document> reciprocalRankFusion(List<Document> knnHits, List<Document> bm25Hits, int topK, int k) {
        Map<String, Double> scoreMap = new HashMap<>(); // ID -> RRF Score
        Map<String, Document> docById = new LinkedHashMap<>(); // ID -> Document Instance

        for (int i = 0; i < knnHits.size(); i++) {
            Document d = knnHits.get(i);
            String id = docId(d);
            scoreMap.put(id, scoreMap.getOrDefault(id, 0.0) + (1.0 / (k + (i + 1))));
            docById.putIfAbsent(id, d);
        }

        for (int i = 0; i < bm25Hits.size(); i++) {
            Document d = bm25Hits.get(i);
            String id = docId(d);
            scoreMap.put(id, scoreMap.getOrDefault(id, 0.0) + (1.0 / (k + (i + 1))));
            docById.putIfAbsent(id, d);
        }

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(topK)
                .map(e -> docById.get(e.getKey()))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 将 Spring AI Document 列表转换为业务 VO 列表
     */
    public List<ChunkSourceVO> mapToVO(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream().map(d -> {
            Long chunkId = Convert.toLong(d.getMetadata().get("chunkId"));
            Long documentId = Convert.toLong(d.getMetadata().get("documentId"));
            String documentName = Convert.toStr(d.getMetadata().get("documentName"));

            return ChunkSourceVO.builder()
                    .chunkId(chunkId)
                    .documentId(documentId)
                    .documentName(documentName)
                    .content(d.getText())
                    .score(d.getScore())
                    .build();
        }).toList();
    }

    private String toFilterQueryString(SearchRequest searchRequest) {
        if (!searchRequest.hasFilterExpression() || searchRequest.getFilterExpression() == null) {
            return "*";
        }
        return filterConverter.convertExpression(searchRequest.getFilterExpression());
    }

    private String docId(Document d) {
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
