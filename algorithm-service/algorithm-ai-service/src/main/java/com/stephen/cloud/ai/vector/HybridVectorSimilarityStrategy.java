package com.stephen.cloud.ai.vector;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.stephen.cloud.ai.annotation.VectorSimilarityType;
import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.model.enums.VectorSimilarityModeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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

/**
 * 混合检索：kNN + BM25，RRF 融合。
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

    @Override
    public List<Document> search(SearchRequest searchRequest) {
        List<Document> knnHits = knowledgeVectorStore.similaritySearch(searchRequest);

        String filterQs = toFilterQueryString(searchRequest);
        int topK = searchRequest.getTopK();
        int fetch = Math.max(topK, knowledgeProperties.getHybridBm25FetchSize());

        List<Document> bm25Hits;
        try {
            bm25Hits = searchBm25(searchRequest.getQuery(), filterQs, fetch);
        } catch (IOException e) {
            log.warn("BM25 检索失败，降级为纯向量检索: {}", e.getMessage());
            return knnHits;
        }

        if (bm25Hits.isEmpty()) {
            return knnHits;
        }

        return reciprocalRankFusion(knnHits, bm25Hits, topK, knowledgeProperties.getRrfRankConstant());
    }

    private String toFilterQueryString(SearchRequest searchRequest) {
        if (!searchRequest.hasFilterExpression() || searchRequest.getFilterExpression() == null) {
            return "*";
        }
        return filterConverter.convertExpression(searchRequest.getFilterExpression());
    }

    private List<Document> searchBm25(String queryText, String filterQueryString, int size) throws IOException {
        SearchResponse<Document> res = elasticsearchClient.search(s -> s
                        .index(knowledgeProperties.getVectorIndex())
                        .size(size)
                        .query(q -> q.bool(b -> {
                            b.filter(f -> f.queryString(qs -> qs.query(filterQueryString)));
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
            if (src == null) {
                continue;
            }
            if (hit.score() != null) {
                out.add(src.mutate().score(hit.score()).build());
            } else {
                out.add(src);
            }
        }
        return out;
    }

    private List<Document> reciprocalRankFusion(List<Document> knnHits, List<Document> bm25Hits, int topK,
            int k) {
        Map<String, Double> scoreMap = new HashMap<>();
        Map<String, Document> docById = new LinkedHashMap<>();

        int rank = 1;
        for (Document d : knnHits) {
            String id = docId(d);
            scoreMap.merge(id, 1.0 / (k + rank), Double::sum);
            docById.putIfAbsent(id, d);
            rank++;
        }
        rank = 1;
        for (Document d : bm25Hits) {
            String id = docId(d);
            scoreMap.merge(id, 1.0 / (k + rank), Double::sum);
            docById.putIfAbsent(id, d);
            rank++;
        }

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(topK)
                .map(e -> docById.get(e.getKey()))
                .filter(Objects::nonNull)
                .toList();
    }

    private static String docId(Document d) {
        return d.getId() != null ? d.getId() : String.valueOf(System.identityHashCode(d));
    }
}
