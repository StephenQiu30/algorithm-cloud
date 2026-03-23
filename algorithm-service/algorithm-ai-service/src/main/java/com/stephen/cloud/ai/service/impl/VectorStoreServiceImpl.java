package com.stephen.cloud.ai.service.impl;

import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.model.enums.VectorSimilarityModeEnum;
import com.stephen.cloud.ai.service.VectorStoreService;
import com.stephen.cloud.ai.vector.VectorSimilarityStrategyRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;

/**
 * 向量存储服务实现：统一维护 {@link VectorStore} 的生命周期与多策略检索。
 * <p>
 * 遵循官方最佳实践：
 * 1. 增量写入采用批量接口。
 * 2. 检索逻辑通过策略注册表解耦 kNN 与 Hybrid。
 * 3. 删除操作基于元数据过滤，确保存储一致性。
 * </p>
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class VectorStoreServiceImpl implements VectorStoreService {

    @Resource
    private VectorStore knowledgeVectorStore;

    @Resource
    private VectorSimilarityStrategyRegistry vectorSimilarityStrategyRegistry;

    @Resource
    private KnowledgeProperties knowledgeProperties;

    private final FilterExpressionTextParser parser = new FilterExpressionTextParser();

    /**
     * 批量向向量库中添加文档分片。
     *
     * @param documents 经过元数据增强的 Spring AI Document 对象列表
     */
    @Override
    public void addDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        log.info("Adding {} documents to knowledge vector store", documents.size());
        knowledgeVectorStore.add(documents);
    }

    /**
     * 执行相似度检索：自动根据配置在 kNN 与 Hybrid (RRF) 策略间切换。
     *
     * @param searchRequest 检索请求配置
     * @return 匹配的文档分片列表
     */
    @Override
    public List<Document> similaritySearch(SearchRequest searchRequest) {
        Assert.notNull(searchRequest, "SearchRequest must not be null");
        
        VectorSimilarityModeEnum mode = knowledgeProperties.isHybridSearchEnabled()
                ? VectorSimilarityModeEnum.HYBRID
                : VectorSimilarityModeEnum.KNN;

        try {
            return vectorSimilarityStrategyRegistry.getStrategy(mode).search(searchRequest);
        } catch (Exception e) {
            log.error("Similarity search failed (mode: {}): {}", mode, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 根据文档 ID 删除向量库中的所有关联分片。
     *
     * @param documentId 文档内码
     */
    @Override
    public void deleteByDocumentId(long documentId) {
        log.info("Deleting vectors for documentId: {}", documentId);
        // 使用元数据过滤器进行精确删除
        Filter.Expression ex = parser.parse("documentId == '" + documentId + "'");
        knowledgeVectorStore.delete(ex);
    }
}
