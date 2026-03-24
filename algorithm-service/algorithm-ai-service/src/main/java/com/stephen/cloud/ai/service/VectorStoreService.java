package com.stephen.cloud.ai.service;

import org.springframework.ai.document.Document;

import java.util.List;

public interface VectorStoreService {

    List<Document> similaritySearch(String query, Long knowledgeBaseId, Integer topK, Double similarityThreshold);

    void deleteByKnowledgeBaseId(Long knowledgeBaseId);

    void deleteByDocumentId(Long documentId);

    List<Document> searchByDocumentId(String query, Long documentId, Integer topK, Double similarityThreshold);
}
