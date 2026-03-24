package com.stephen.cloud.ai.service;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

public interface KeywordSearchService {

    List<Document> bm25Search(String query, Long knowledgeBaseId, Integer topK, Map<String, String> metadataFilters);
}
