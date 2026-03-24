package com.stephen.cloud.ai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;

public interface KeywordSearchService {

    List<Document> bm25Search(String query, Long knowledgeBaseId, Integer topK, Filter.Expression filterExpression);
}
