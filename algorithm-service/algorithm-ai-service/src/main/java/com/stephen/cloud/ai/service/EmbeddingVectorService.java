package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.model.entity.EmbeddingVector;

import java.util.List;

/**
 * 向量元数据审计服务：记录向量化过程，包括使用的模型、维度及 ES 文档 ID。
 *
 * @author StephenQiu30
 */
public interface EmbeddingVectorService extends IService<EmbeddingVector> {

    /**
     * 批量保存向量化审计日志。
     *
     * @param chunks    对应的文档分片列表（需带 ID）
     * @param model     使用的 Embedding 模型
     * @param dimension 向量维度
     * @param esDocIds  ES 中的文档 ID 列表
     */
    void batchSaveAuditLogs(List<DocumentChunk> chunks, String model, int dimension, List<String> esDocIds);

    /**
     * 清理文档相关的向量元数据。
     *
     * @param docId 文档 ID
     */
    void deleteByDocumentId(Long docId);
}
