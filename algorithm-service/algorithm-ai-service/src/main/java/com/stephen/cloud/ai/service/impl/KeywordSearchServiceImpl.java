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

import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.*;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * chunk 索引 BM25 关键词检索服务
 * <p>
 * 搜索字段包含 content（主内容）、documentName（文档名加权）、sectionTitle（章节标题加权），
 * 提升标题级别的关键词召回能力。
 * </p>
 *
 * @author StephenQiu30
 */
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
        // 多字段搜索：content 为主、documentName 权重 2x、sectionTitle 权重 1.5x
        boolBuilder.must(m -> m.multiMatch(mm -> mm
                .fields("content", DOCUMENT_NAME + "^2", SECTION_TITLE + "^1.5")
                .query(query)
                .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
        ));
        if (filterExpression != null) {
            Query filterQuery = filterConverter.convert(filterExpression);
            if (filterQuery != null) {
                boolBuilder.filter(filterQuery);
            }
        }

        String indexName = StringUtils.defaultIfBlank(
                ragRetrievalProperties.getKeywordIndexName(),
                StringUtils.defaultIfBlank(ragRetrievalProperties.getIndexName(), EsIndexConstant.CHUNK_INDEX)
        );
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(indexName)
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
                    addMetadataIfPresent(source, metadata, DOCUMENT_ID);
                    addMetadataIfPresent(source, metadata, DOCUMENT_NAME);
                    addMetadataIfPresent(source, metadata, CHUNK_INDEX);
                    addMetadataIfPresent(source, metadata, KNOWLEDGE_BASE_ID);
                    addMetadataIfPresent(source, metadata, SECTION_TITLE);
                    addMetadataIfPresent(source, metadata, SECTION_PATH);

                    String chunkId = resolveStableChunkId(metadata, hit.id());
                    metadata.put(CHUNK_ID, chunkId);
                    metadata.putIfAbsent(VECTOR_ID, chunkId);
                    metadata.put(KEYWORD_SCORE, hit.score());
                    metadata.put(KEYWORD_RANK, rank++);
                    metadata.put(SOURCE_TYPE, "keyword");
                    metadata.put(ES_ID, hit.id());
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
        Object chunkId = metadata.get(CHUNK_ID);
        if (chunkId != null && StringUtils.isNotBlank(String.valueOf(chunkId))) {
            return String.valueOf(chunkId);
        }
        Object vectorId = metadata.get(VECTOR_ID);
        if (vectorId != null && StringUtils.isNotBlank(String.valueOf(vectorId))) {
            return String.valueOf(vectorId);
        }
        Document probe = new Document(fallbackId, "", metadata);
        String resolved = ragDocumentHelper.resolveChunkId(probe);
        return StringUtils.defaultIfBlank(resolved, fallbackId);
    }
}
