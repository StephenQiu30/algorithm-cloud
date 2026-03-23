package com.stephen.cloud.ai.service.impl;

import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.api.knowledge.model.enums.VectorSimilarityModeEnum;
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
     * 批量向向量库中添加文档分片
     * <p>
     * 文档入库后会自动通过 Embedding Model 转换为稠密向量。
     * 建议 Metadata 中必须携带 {@code knowledgeBaseId} 以支持后续的多租户硬过滤。
     * </p>
     *
     * @param documents 包含正文与关键 Metadata 的 Spring AI Document 对象列表
     */
    @Override
    public void addDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        log.info("正在向向量库同步 {} 条文档记录", documents.size());
        knowledgeVectorStore.add(documents);
    }

    /**
     * 执行向量空间相似度检索
     * <p>
     * 策略选择逻辑：
     * 1. 检查 `knowledge.hybrid-search-enabled` 配置。
     * 2. 若配置开启，则从中获取 HYBRID 策略（kNN + BM25 并通过 RRF 融合）。
     * 3. 否则回退至传统的跨索引 kNN 语义检索。
     * </p>
     *
     * @param searchRequest 封装了 Query、Filter Expression 及 TopK 的检索配置
     * @return 命中的 Document 列表（包含相似度分数 metadata）
     */
    @Override
    public List<Document> similaritySearch(SearchRequest searchRequest) {
        Assert.notNull(searchRequest, "检索请求对象不能为空");
        
        // 根据系统全局配置或动态条件决定检索模式
        VectorSimilarityModeEnum mode = knowledgeProperties.isHybridSearchEnabled()
                ? VectorSimilarityModeEnum.HYBRID
                : VectorSimilarityModeEnum.KNN;

        try {
            return vectorSimilarityStrategyRegistry.getStrategy(mode).search(searchRequest);
        } catch (Exception e) {
            log.error("向量检索执行异常 (模式: {}): {}", mode, e.getMessage(), e);
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
