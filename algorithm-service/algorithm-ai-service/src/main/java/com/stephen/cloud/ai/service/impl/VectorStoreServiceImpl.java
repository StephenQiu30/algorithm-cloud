package com.stephen.cloud.ai.service.impl;

import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.model.enums.VectorSimilarityModeEnum;
import com.stephen.cloud.ai.service.VectorStoreService;
import com.stephen.cloud.ai.vector.VectorSimilarityStrategyRegistry;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 向量存储服务实现：统一 {@link VectorStore} 的增删与可选混合检索（委托
 * {@link com.stephen.cloud.ai.vector.VectorSimilarityStrategyRegistry}）。
 *
 * @author StephenQiu30
 */
@Service
public class VectorStoreServiceImpl implements VectorStoreService {

    @Resource
    private VectorStore knowledgeVectorStore;

    @Resource
    private VectorSimilarityStrategyRegistry vectorSimilarityStrategyRegistry;

    @Resource
    private KnowledgeProperties knowledgeProperties;

    private final FilterExpressionTextParser parser = new FilterExpressionTextParser();

    @Override
    public void addDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        knowledgeVectorStore.add(documents);
    }

    @Override
    public List<Document> similaritySearch(SearchRequest searchRequest) {
        VectorSimilarityModeEnum mode = knowledgeProperties.isHybridSearchEnabled()
                ? VectorSimilarityModeEnum.HYBRID
                : VectorSimilarityModeEnum.KNN;
        return vectorSimilarityStrategyRegistry.getStrategy(mode).search(searchRequest);
    }

    @Override
    public void deleteByDocumentId(long documentId) {
        Filter.Expression ex = parser.parse("documentId == '" + documentId + "'");
        knowledgeVectorStore.delete(ex);
    }
}
