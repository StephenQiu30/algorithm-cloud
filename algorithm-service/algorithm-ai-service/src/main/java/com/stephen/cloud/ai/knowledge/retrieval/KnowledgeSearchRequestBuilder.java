package com.stephen.cloud.ai.knowledge.retrieval;

import com.stephen.cloud.ai.config.KnowledgeProperties;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeSearchRequestBuilder {

    @Resource
    private KnowledgeProperties knowledgeProperties;

    public int resolveTopK(Integer requestTopK) {
        int topK = requestTopK != null && requestTopK > 0 ? requestTopK : knowledgeProperties.getDefaultTopK();
        return Math.min(topK, knowledgeProperties.getRetrievalTopKMax());
    }

    public SearchRequest build(String query, Long knowledgeBaseId, Integer requestTopK) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        return SearchRequest.builder()
                .query(StringUtils.trim(query))
                .topK(resolveTopK(requestTopK))
                .similarityThreshold(knowledgeProperties.getVectorSimilarityThreshold())
                .filterExpression(builder.eq("knowledgeBaseId", String.valueOf(knowledgeBaseId)).build())
                .build();
    }
}
