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
import com.stephen.cloud.ai.knowledge.retrieval.RagDocumentHelper;
import com.stephen.cloud.ai.service.KeywordSearchService;
import com.stephen.cloud.api.search.constant.EsIndexConstant;
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

/** chunk 索引 BM25，字段与索引名同 ChunkEsDTO / EsIndexConstant（与 search 写入一致）。 */
@Service
@Slf4j
public class KeywordSearchServiceImpl implements KeywordSearchService {

    @Resource
    private ElasticsearchClient elasticsearchClient;

    @Resource
    private RagRetrievalProperties ragRetrievalProperties;

    @Resource
    private RagDocumentHelper ragDocumentHelper;

    private final ElasticsearchFilterExpressionConverter filterConverter = new ElasticsearchFilterExpressionConverter();

    @Override
    public List<Document> bm25Search(String query, Integer topK, Filter.Expression filterExpression) {
        if (StringUtils.isBlank(query)) {
            return List.of();
        }
        int defaultTopK = ragRetrievalProperties.getKeywordTopK() <= 0 ? 10 : ragRetrievalProperties.getKeywordTopK();
        int finalTopK = topK == null || topK <= 0 ? defaultTopK : topK;

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        boolBuilder.filter(f -> f.term(t -> t.field("isDelete").value(0)));
        boolBuilder.must(m -> m.multiMatch(mm -> mm
                .fields("content")
                .query(query)
                .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
        ));
        if (filterExpression != null) {
            Query filterQuery = filterConverter.convert(filterExpression);
            if (filterQuery != null) {
                boolBuilder.filter(filterQuery);
            }
        }

        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(EsIndexConstant.CHUNK_INDEX)
                .query(boolBuilder.build()._toQuery())
                .size(finalTopK)
                .build();

        try {
            SearchResponse<Map<String, Object>> response = elasticsearchClient.search(searchRequest,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);
            List<Document> results = new ArrayList<>();
            int rank = 1;

            if (response.hits() != null && response.hits().hits() != null) {
                for (Hit<Map<String, Object>> hit : response.hits().hits()) {
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

                    String chunkId = resolveStableChunkId(metadata, hit.id());
                    metadata.put("chunkId", chunkId);
                    metadata.putIfAbsent("vectorId", chunkId);
                    metadata.put("keywordScore", hit.score());
                    metadata.put("keywordRank", rank++);
                    metadata.put("sourceType", "keyword");
                    metadata.put("esId", hit.id());
                    results.add(new Document(chunkId, text, metadata));
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

    private String resolveStableChunkId(Map<String, Object> metadata, String fallbackId) {
        Object chunkId = metadata.get("chunkId");
        if (chunkId != null && StringUtils.isNotBlank(String.valueOf(chunkId))) {
            return String.valueOf(chunkId);
        }
        Object vectorId = metadata.get("vectorId");
        if (vectorId != null && StringUtils.isNotBlank(String.valueOf(vectorId))) {
            return String.valueOf(vectorId);
        }
        Document probe = new Document(fallbackId, "", metadata);
        String resolved = ragDocumentHelper.resolveChunkId(probe);
        return StringUtils.defaultIfBlank(resolved, fallbackId);
    }
}
