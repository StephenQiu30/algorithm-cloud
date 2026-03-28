package com.stephen.cloud.ai.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 向量存储服务接口
 * <p>
 * 提供向量检索和向量数据管理能力
 * </p>
 *
 * @author StephenQiu30
 */
public interface VectorStoreService {

    /**
     * 相似度检索
     *
     * @param query              查询文本
     * @param knowledgeBaseId    知识库 ID
     * @param topK               返回数量
     * @param similarityThreshold 相似度阈值
     * @return 相似的文档片段列表
     */
    List<Document> similaritySearch(String query, Long knowledgeBaseId, Integer topK, Double similarityThreshold);

    /**
     * 删除指定知识库的所有向量数据
     *
     * @param knowledgeBaseId 知识库 ID
     */
    void deleteByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * 删除指定文档的所有向量数据
     *
     * @param documentId 文档 ID
     */
    void deleteByDocumentId(Long documentId);

    /**
     * 在指定文档范围内进行相似度检索
     *
     * @param query              查询文本
     * @param documentId         文档 ID
     * @param topK               返回数量
     * @param similarityThreshold 相似度阈值
     * @return 相似的文档片段列表
     */
    List<Document> searchByDocumentId(String query, Long documentId, Integer topK, Double similarityThreshold);
}
