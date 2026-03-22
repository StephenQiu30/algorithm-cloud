package com.stephen.cloud.ai.vector;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;

import java.util.List;

/**
 * 向量相似度检索策略（纯 kNN 或混合）。
 *
 * @author StephenQiu30
 */
public interface VectorSimilaritySearchStrategy {

    List<Document> search(SearchRequest searchRequest);
}
