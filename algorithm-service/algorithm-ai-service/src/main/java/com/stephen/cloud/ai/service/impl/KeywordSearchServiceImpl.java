package com.stephen.cloud.ai.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.stephen.cloud.ai.config.RagRetrievalProperties;
import com.stephen.cloud.ai.knowledge.retrieval.ElasticsearchFilterExpressionConverter;
import com.stephen.cloud.ai.service.KeywordSearchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KeywordSearchServiceImpl implements KeywordSearchService {

    @Resource
    private ElasticsearchClient elasticsearchClient;

    @Resource
    private RagRetrievalProperties ragRetrievalProperties;

    private final ElasticsearchFilterExpressionConverter filterConverter = new ElasticsearchFilterExpressionConverter();

    @Override
    public List<Document> bm25Search(String query, Long knowledgeBaseId, Integer topK, Filter.Expression filterExpression) {
        if (StringUtils.isBlank(query)) {
            return List.of();
        }
        int defaultTopK = ragRetrievalProperties.getKeywordTopK() <= 0 ? 10 : ragRetrievalProperties.getKeywordTopK();
        int finalTopK = topK == null || topK <= 0 ? defaultTopK : topK;

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        boolBuilder.must(m -> m.multiMatch(mm -> mm
                .fields("content", "metadata.documentName")
                .query(query)
                .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
        ));

        // 应用知识库过滤
        if (knowledgeBaseId != null && knowledgeBaseId > 0) {
            boolBuilder.filter(f -> f.bool(b -> b
                    .should(s -> s.term(t -> t.field("metadata.knowledgeBaseId").value(String.valueOf(knowledgeBaseId))))
                    .should(s -> s.term(t -> t.field("knowledgeBaseId").value(knowledgeBaseId)))
                    .minimumShouldMatch("1")
            ));
        }

        // 应用 Spring AI 表达式过滤
        if (filterExpression != null) {
            Query filterQuery = filterConverter.convert(filterExpression);
            if (filterQuery != null) {
                boolBuilder.filter(filterQuery);
            }
        }

        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(ragRetrievalProperties.getIndexName())
                .query(boolBuilder.build()._toQuery())
                .size(finalTopK)
                .build();

        try {
            SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);
            List<Document> results = new ArrayList<>();
            int rank = 1;

            if (response.hits() != null && response.hits().hits() != null) {
                for (Hit<Map> hit : response.hits().hits()) {
                    Map<String, Object> source = hit.source();
                    if (source == null) continue;

                    String text = source.get("content") == null ? "" : String.valueOf(source.get("content"));
                    Map<String, Object> metadata = new HashMap<>();
                    Object metadataObj = source.get("metadata");
                    if (metadataObj instanceof Map<?, ?> sourceMetadata) {
                        sourceMetadata.forEach((k, v) -> metadata.put(String.valueOf(k), v));
                    }
                    // 补充一级字段到 metadata（兼容性）
                    addMetadataIfPresent(source, metadata, "documentId");
                    addMetadataIfPresent(source, metadata, "documentName");
                    addMetadataIfPresent(source, metadata, "chunkIndex");
                    addMetadataIfPresent(source, metadata, "knowledgeBaseId");

                    metadata.put("keywordScore", hit.score());
                    metadata.put("keywordRank", rank++);
                    metadata.put("sourceType", "keyword");
                    metadata.put("esId", hit.id());
                    results.add(new Document(text, metadata));
                }
            }
            return results;
        } catch (IOException | ElasticsearchException e) {
            log.error("[KeywordSearch] 搜索失败: query={}, error={}", query, e.getMessage());
            return List.of();
        }
    }

    private void addMetadataIfPresent(Map<String, Object> source, Map<String, Object> metadata, String key) {
        if (source.get(key) != null) {
            metadata.putIfAbsent(key, source.get(key));
        }
    }
}
