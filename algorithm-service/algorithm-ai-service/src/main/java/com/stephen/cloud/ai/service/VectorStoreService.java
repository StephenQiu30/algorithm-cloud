package com.stephen.cloud.ai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;

import java.util.List;

/**
 * 知识库向量存储封装（统一 Spring AI {@link org.springframework.ai.vectorstore.VectorStore} 操作）。
 * <p>
 * {@link #similaritySearch} 在配置开启混合检索时经 {@link com.stephen.cloud.ai.vector.VectorSimilarityStrategyRegistry}
 * 选择 {@link com.stephen.cloud.api.knowledge.model.enums.VectorSimilarityModeEnum#HYBRID} 策略（kNN + BM25 + RRF），
 * 与 {@link RagService}、{@link KnowledgeRetrievalService} 共用同一路径，保证入库 metadata 与检索过滤一致。
 * </p>
 *
 * @author StephenQiu30
 */
public interface VectorStoreService {

    /**
     * 批量写入文档向量（内部会调用嵌入模型）。metadata 需含 knowledgeBaseId、documentId、chunkId 等以便过滤与溯源。
     *
     * @param documents 带文本与 metadata 的文档列表
     */
    void addDocuments(List<Document> documents);

    /**
     * 相似度检索；开启混合检索时为 kNN + BM25 经 RRF 融合后的结果。
     *
     * @param searchRequest 查询文本、topK、阈值、metadata 过滤表达式等
     * @return 命中文档列表（含相似度分数）
     */
    List<Document> similaritySearch(SearchRequest searchRequest);

    /**
     * 按业务文档 ID（metadata 中的 documentId）删除该文档下全部向量
     *
     * @param documentId 知识库文档主键
     */
    void deleteByDocumentId(long documentId);
}
