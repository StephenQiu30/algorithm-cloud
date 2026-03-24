package com.stephen.cloud.ai.knowledge.retrieval;

import cn.hutool.core.convert.Convert;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.enums.RagMetricEnum;
import com.stephen.cloud.api.knowledge.model.enums.RagMetricTagEnum;
import com.stephen.cloud.api.knowledge.model.enums.VectorSimilarityModeEnum;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
import java.util.stream.Collectors;

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

    @Resource
    private MeterRegistry meterRegistry;

    /**
     * 核心检索逻辑
     */
    public List<Document> search(SearchRequest searchRequest, VectorSimilarityModeEnum mode) {
        long start = System.currentTimeMillis();
        List<Document> out = mode == VectorSimilarityModeEnum.HYBRID ? searchHybrid(searchRequest) : searchKnn(searchRequest);
        Timer.builder(RagMetricEnum.RAG_RETRIEVAL_LATENCY_MS.getValue())
                .tag(RagMetricTagEnum.MODE_TAG_KEY.getValue(), mode.name())
                .register(meterRegistry)
                .record(System.currentTimeMillis() - start, java.util.concurrent.TimeUnit.MILLISECONDS);
        Counter.builder(RagMetricEnum.RAG_RETRIEVAL_HIT_COUNT.getValue())
                .tag(RagMetricTagEnum.MODE_TAG_KEY.getValue(), mode.name())
                .register(meterRegistry)
                .increment(out != null ? out.size() : 0);
        return out;
    }

    public Map<String, List<Document>> diagnoseHybrid(SearchRequest searchRequest) {
        String filterQs = toFilterQueryString(searchRequest);
        int topK = searchRequest.getTopK();
        int fetch = Math.max(topK, knowledgeProperties.getHybridBm25FetchSize());
        List<Document> knnHits = searchKnn(searchRequest);
        List<Document> bm25Hits;
        try {
            bm25Hits = searchBm25(searchRequest.getQuery(), filterQs, fetch, knowledgeProperties.getBm25ScoreThreshold());
        } catch (IOException e) {
            log.warn("BM25 retrieval failed during diagnose: {}", e.getMessage());
            bm25Hits = List.of();
        }
        List<Document> hybridHits = reciprocalRankFusion(knnHits, bm25Hits, topK, knowledgeProperties.getRrfRankConstant());
        Map<String, List<Document>> result = new LinkedHashMap<>();
        result.put("knn", knnHits);
        result.put("bm25", bm25Hits);
        result.put("hybrid", hybridHits);
        return result;
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
                return searchBm25(searchRequest.getQuery(), filterQs, fetch, knowledgeProperties.getBm25ScoreThreshold());
            } catch (IOException e) {
                log.error("[检索链路] BM25检索IO异常: query='{}', error={}", searchRequest.getQuery(), e.getMessage(), e);
                return new ArrayList<>();
            } catch (Exception e) {
                log.error("[检索链路] BM25检索未知异常: query='{}', error={}", searchRequest.getQuery(), e.getMessage(), e);
                return new ArrayList<>();
            }
        }, vectorHybridSearchExecutor);

        try {
            CompletableFuture.allOf(knnFuture, bm25Future).join();
            List<Document> knnHits = knnFuture.get();
            List<Document> bm25Hits = bm25Future.get();

            if (bm25Hits == null || bm25Hits.isEmpty()) {
                Counter.builder(RagMetricEnum.RAG_RETRIEVAL_FALLBACK_COUNT.getValue())
                        .tag(RagMetricTagEnum.MODE_TAG_KEY.getValue(), VectorSimilarityModeEnum.HYBRID.name())
                        .register(meterRegistry)
                        .increment();
                log.warn("[检索链路] BM25返回为空，降级为纯kNN检索: query='{}', topK={}, knnHits={}",
                        searchRequest.getQuery(), topK, knnHits != null ? knnHits.size() : 0);
                return knnHits;
            }

            List<Document> fused = reciprocalRankFusion(knnHits, bm25Hits, topK, knowledgeProperties.getRrfRankConstant());
            log.info("[检索链路] hybrid fused: query='{}', topK={}, knnHits={}, bm25Hits={}, fusedHits={}, knnTopIds={}, bm25TopIds={}, fusedTopIds={}",
                    searchRequest.getQuery(),
                    topK,
                    knnHits.size(),
                    bm25Hits.size(),
                    fused.size(),
                    topIds(knnHits, 5),
                    topIds(bm25Hits, 5),
                    topIds(fused, 5));
            return fused;
        } catch (Exception e) {
            log.error("[检索链路] 混合检索执行异常: query='{}', error={}", searchRequest.getQuery(), e.getMessage(), e);
            // 降级到kNN
            try {
                List<Document> fallbackResult = knnFuture.isCompletedExceptionally() ? new ArrayList<>() : knnFuture.join();
                Counter.builder(RagMetricEnum.RAG_RETRIEVAL_FALLBACK_COUNT.getValue())
                        .tag(RagMetricTagEnum.MODE_TAG_KEY.getValue(), VectorSimilarityModeEnum.HYBRID.name())
                        .register(meterRegistry)
                        .increment();
                log.warn("[检索链路] 混合检索异常，降级为kNN: query='{}', knnHits={}",
                        searchRequest.getQuery(), fallbackResult.size());
                return fallbackResult;
            } catch (Exception fallbackEx) {
                log.error("[检索链路] kNN降级也失败: query='{}', error={}", searchRequest.getQuery(), fallbackEx.getMessage(), fallbackEx);
                return new ArrayList<>();
            }
        }
    }

    /**
     * BM25 文本检索（优化版：增强字段权重，支持标签检索）
     */
    private List<Document> searchBm25(String queryText, String filterQs, int size, double threshold) throws IOException {
        SearchResponse<Document> res = elasticsearchClient.search(s -> s
                        .index(knowledgeProperties.getVectorIndex())
                        .size(size)
                        .query(q -> q.bool(b -> {
                            b.filter(f -> f.queryString(qs -> qs.query(filterQs)));
                            // 优化：增强字段权重，新增 tags 和 tag_list 字段
                            b.must(m -> m.multiMatch(mm -> mm
                                    .query(queryText)
                                    .fields(
                                            CONTENT_FIELD,              // 权重 1
                                            "documentName^2",           // 权重 2
                                            "excerpt_keywords^5",       // 权重 5（LLM 提取的关键词）
                                            "tags^4",                   // 权重 4（规则提取的标签）
                                            "tag_list^4"                // 权重 4（标签数组）
                                    )
                                    .type(TextQueryType.BestFields)
                                    .lenient(true)
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

    private List<String> topIds(List<Document> docs, int limit) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }
        return docs.stream()
                .limit(limit)
                .map(this::docId)
                .collect(Collectors.toList());
    }
}
