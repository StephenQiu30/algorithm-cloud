package com.stephen.cloud.ai.service.impl;

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

import java.util.List;

@Service
@Slf4j
public class VectorStoreServiceImpl implements VectorStoreService {

    @Resource
    private VectorStore vectorStore;

    @Resource
    private RagRetrievalProperties ragRetrievalProperties;

    @Override
    public List<Document> similaritySearch(String query, Filter.Expression filterExpression, Integer topK,
            Double similarityThreshold) {
        int defaultTopK = ragRetrievalProperties.getTopK() <= 0 ? 5 : ragRetrievalProperties.getTopK();
        int finalTopK = topK == null || topK <= 0 ? defaultTopK : topK;
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(finalTopK);
        if (filterExpression != null) {
            builder.filterExpression(filterExpression);
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
        vectorStore.delete(new FilterExpressionBuilder().eq("knowledgeBaseId", knowledgeBaseId).build());
        log.info("[VectorStore] deleteByKnowledgeBaseId success, knowledgeBaseId={}", knowledgeBaseId);
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        if (documentId == null || documentId <= 0) {
            return;
        }
        vectorStore.delete(new FilterExpressionBuilder().eq("documentId", documentId).build());
        log.info("[VectorStore] deleteByDocumentId success, documentId={}", documentId);
    }

    @Override
    public List<Document> searchByDocumentId(String query, Long documentId, Integer topK,
            Double similarityThreshold) {
        Filter.Expression filterExpression = null;
        if (documentId != null && documentId > 0) {
            filterExpression = new FilterExpressionBuilder().eq("documentId", documentId).build();
        }
        return similaritySearch(query, filterExpression, topK, similarityThreshold);
    }

}
