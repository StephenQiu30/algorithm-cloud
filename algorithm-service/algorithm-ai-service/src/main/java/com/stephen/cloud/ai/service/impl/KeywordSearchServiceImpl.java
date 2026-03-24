package com.stephen.cloud.ai.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.stephen.cloud.ai.config.RagRetrievalProperties;
import com.stephen.cloud.ai.service.KeywordSearchService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KeywordSearchServiceImpl implements KeywordSearchService {

    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    @Resource
    private RagRetrievalProperties ragRetrievalProperties;

    @Override
    public List<Document> bm25Search(String query, Long knowledgeBaseId, Integer topK, Map<String, String> metadataFilters) {
        if (StringUtils.isBlank(query)) {
            return List.of();
        }
        int defaultTopK = ragRetrievalProperties.getKeywordTopK() <= 0 ? 10 : ragRetrievalProperties.getKeywordTopK();
        int finalTopK = topK == null || topK <= 0 ? defaultTopK : topK;
        Query finalQuery;
        if (knowledgeBaseId != null && knowledgeBaseId > 0) {
            Query kbFilterQuery = Query.of(q -> q.bool(b -> b
                    .should(s -> s.term(t -> t.field("metadata.knowledgeBaseId").value(String.valueOf(knowledgeBaseId))))
                    .should(s -> s.term(t -> t.field("knowledgeBaseId").value(knowledgeBaseId)))
                    .minimumShouldMatch("1")
            ));
            finalQuery = Query.of(q -> q.bool(b -> {
                b.must(m -> m.match(mm -> mm.field("content").query(query)));
                b.filter(kbFilterQuery);
                applyMetadataFilters(b, metadataFilters);
                return b;
            }));
        } else {
            finalQuery = Query.of(q -> q.bool(b -> {
                b.must(m -> m.match(mm -> mm.field("content").query(query)));
                applyMetadataFilters(b, metadataFilters);
                return b;
            }));
        }
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(finalQuery)
                .withPageable(PageRequest.of(0, finalTopK))
                .build();
        SearchHits<Map> searchHits = elasticsearchOperations.search(nativeQuery, Map.class,
                IndexCoordinates.of(ragRetrievalProperties.getIndexName()));
        List<Document> results = new ArrayList<>();
        int rank = 1;
        for (SearchHit<Map> hit : searchHits) {
            Map<String, Object> source = hit.getContent();
            String text = source.get("content") == null ? "" : String.valueOf(source.get("content"));
            Map<String, Object> metadata = new HashMap<>();
            Object metadataObj = source.get("metadata");
            if (metadataObj instanceof Map<?, ?> sourceMetadata) {
                sourceMetadata.forEach((k, v) -> metadata.put(String.valueOf(k), v));
            }
            if (source.get("documentId") != null) {
                metadata.putIfAbsent("documentId", source.get("documentId"));
            }
            if (source.get("documentName") != null) {
                metadata.putIfAbsent("documentName", source.get("documentName"));
            }
            if (source.get("chunkIndex") != null) {
                metadata.putIfAbsent("chunkIndex", source.get("chunkIndex"));
            }
            if (source.get("knowledgeBaseId") != null) {
                metadata.putIfAbsent("knowledgeBaseId", source.get("knowledgeBaseId"));
            }
            metadata.put("keywordScore", hit.getScore());
            metadata.put("keywordRank", rank++);
            metadata.put("sourceType", "keyword");
            metadata.put("esId", hit.getId());
            Document document = new Document(text, metadata);
            results.add(document);
        }
        return results;
    }

    private void applyMetadataFilters(co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder builder,
                                      Map<String, String> metadataFilters) {
        if (metadataFilters == null || metadataFilters.isEmpty()) {
            return;
        }
        String version = metadataFilters.get("version");
        if (StringUtils.isNotBlank(version)) {
            builder.filter(f -> f.bool(b -> b
                    .should(s -> s.term(t -> t.field("metadata.version").value(version)))
                    .should(s -> s.term(t -> t.field("version").value(version)))
                    .minimumShouldMatch("1")));
        }
        String bizTag = metadataFilters.get("bizTag");
        if (StringUtils.isNotBlank(bizTag)) {
            builder.filter(f -> f.bool(b -> b
                    .should(s -> s.term(t -> t.field("metadata.bizTag").value(bizTag)))
                    .should(s -> s.term(t -> t.field("bizTag").value(bizTag)))
                    .minimumShouldMatch("1")));
        }
    }
}
