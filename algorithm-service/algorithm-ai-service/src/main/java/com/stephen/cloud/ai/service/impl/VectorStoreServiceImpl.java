package com.stephen.cloud.ai.service.impl;

import com.stephen.cloud.ai.service.VectorStoreService;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorStoreServiceImpl implements VectorStoreService {

    @Resource
    private VectorStore knowledgeVectorStore;

    @Override
    public void addDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        knowledgeVectorStore.add(documents);
    }

    @Override
    public List<Document> similaritySearch(SearchRequest searchRequest) {
        return knowledgeVectorStore.similaritySearch(searchRequest);
    }

    @Override
    public void deleteByDocumentId(long documentId) {
        FilterExpressionTextParser parser = new FilterExpressionTextParser();
        Filter.Expression ex = parser.parse("documentId == '" + documentId + "'");
        knowledgeVectorStore.delete(ex);
    }
}
