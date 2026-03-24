package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.stephen.cloud.ai.config.RagRetrievalProperties;
import com.stephen.cloud.ai.service.VectorStoreService;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorStoreServiceImpl implements VectorStoreService {

    @Resource
    private VectorStore vectorStore;

    @Resource
    private RagRetrievalProperties ragRetrievalProperties;

    @Override
    public List<Document> similaritySearch(String query, Long knowledgeBaseId, Integer topK, Double similarityThreshold) {
        int defaultTopK = ragRetrievalProperties.getTopK() <= 0 ? 5 : ragRetrievalProperties.getTopK();
        int finalTopK = topK == null || topK <= 0 ? defaultTopK : topK;
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(finalTopK);
        if (knowledgeBaseId != null && knowledgeBaseId > 0) {
            builder.filterExpression("knowledgeBaseId == " + knowledgeBaseId);
        }
        Double defaultSimilarityThreshold = ragRetrievalProperties.getSimilarityThreshold();
        Double finalSimilarityThreshold = similarityThreshold == null ? defaultSimilarityThreshold : similarityThreshold;
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
        deleteByFilter("knowledgeBaseId == " + knowledgeBaseId);
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        if (documentId == null || documentId <= 0) {
            return;
        }
        deleteByFilter("documentId == " + documentId);
    }

    private void deleteByFilter(String filterExpression) {
        if (CollUtil.isEmpty(vectorStore.similaritySearch(
                SearchRequest.builder().query("test").topK(1).filterExpression(filterExpression).build()
        ))) {
            return;
        }
        vectorStore.delete(filterExpression);
    }
}
