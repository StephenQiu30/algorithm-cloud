package com.stephen.cloud.ai.vector;

import com.stephen.cloud.ai.annotation.VectorSimilarityType;
import com.stephen.cloud.api.knowledge.model.enums.VectorSimilarityModeEnum;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 纯向量 kNN 检索。
 *
 * @author StephenQiu30
 */
@VectorSimilarityType(VectorSimilarityModeEnum.KNN)
@Component
public class KnnVectorSimilarityStrategy implements VectorSimilaritySearchStrategy {

    @Resource
    private VectorStore knowledgeVectorStore;

    @Override
    public List<Document> search(SearchRequest searchRequest) {
        return knowledgeVectorStore.similaritySearch(searchRequest);
    }
}
