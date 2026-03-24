package com.stephen.cloud.ai.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.stephen.cloud.ai.config.RagRetrievalProperties;
import com.stephen.cloud.ai.service.VectorStoreService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class VectorStoreServiceImpl implements VectorStoreService {

    @Resource
    private VectorStore vectorStore;

    @Resource
    private RagRetrievalProperties ragRetrievalProperties;

    @Resource
    private ElasticsearchClient elasticsearchClient;

    @Override
    public List<Document> similaritySearch(String query, Long knowledgeBaseId, Integer topK,
            Double similarityThreshold) {
        int defaultTopK = ragRetrievalProperties.getTopK() <= 0 ? 5 : ragRetrievalProperties.getTopK();
        int finalTopK = topK == null || topK <= 0 ? defaultTopK : topK;
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(finalTopK);
        if (knowledgeBaseId != null && knowledgeBaseId > 0) {
            builder.filterExpression(new FilterExpressionBuilder().eq("knowledgeBaseId", knowledgeBaseId).build());
        }
        Double defaultSimilarityThreshold = ragRetrievalProperties.getSimilarityThreshold();
        Double finalSimilarityThreshold = similarityThreshold == null ? defaultSimilarityThreshold
                : similarityThreshold;
        if (finalSimilarityThreshold != null && finalSimilarityThreshold > 0 && finalSimilarityThreshold <= 1) {
            builder.similarityThreshold(finalSimilarityThreshold);
        }
        return vectorStore.similaritySearch(builder.build());
    }

    @Override
    public void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        if (knowledgeBaseId == null || knowledgeBaseId <= 0) {
            return;
        }
        deleteByQuery("knowledgeBaseId", knowledgeBaseId);
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        if (documentId == null || documentId <= 0) {
            return;
        }
        deleteByQuery("documentId", documentId);
    }

    private void deleteByQuery(String field, Long value) {
        String indexName = ragRetrievalProperties.getIndexName();
        try {
            var response = elasticsearchClient.deleteByQuery(d -> d
                    .index(indexName)
                    .query(q -> q.queryString(qs -> qs.query(
                            field + ":" + value + " OR metadata." + field + ":" + value
                    )))
            );
            log.info("[VectorStore] deleteByQuery success, index={}, field={}, value={}, deleted={}",
                    indexName, field, value, response.deleted());
        } catch (IOException e) {
            throw new RuntimeException("向量删除失败: field=" + field + ", value=" + value, e);
        }
    }

    @Override
    public List<Document> searchByDocumentId(String query, Long documentId, Integer topK,
            Double similarityThreshold) {
        int defaultTopK = ragRetrievalProperties.getTopK() <= 0 ? 5 : ragRetrievalProperties.getTopK();
        int finalTopK = topK == null || topK <= 0 ? defaultTopK : topK;
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(finalTopK);
        if (documentId != null && documentId > 0) {
            builder.filterExpression(new FilterExpressionBuilder().eq("documentId", documentId).build());
        }
        Double defaultSimilarityThreshold = ragRetrievalProperties.getSimilarityThreshold();
        Double finalSimilarityThreshold = similarityThreshold == null ? defaultSimilarityThreshold
                : similarityThreshold;
        if (finalSimilarityThreshold != null && finalSimilarityThreshold > 0 && finalSimilarityThreshold <= 1) {
            builder.similarityThreshold(finalSimilarityThreshold);
        }
        return vectorStore.similaritySearch(builder.build());
    }

}
