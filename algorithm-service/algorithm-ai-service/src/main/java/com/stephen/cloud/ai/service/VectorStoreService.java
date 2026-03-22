package com.stephen.cloud.ai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;

import java.util.List;

public interface VectorStoreService {

    void addDocuments(List<Document> documents);

    List<Document> similaritySearch(SearchRequest searchRequest);

    void deleteByDocumentId(long documentId);
}
