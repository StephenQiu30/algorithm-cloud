package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.model.entity.EmbeddingVector;
import com.stephen.cloud.api.knowledge.model.dto.vector.EmbeddingVectorQueryRequest;

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
    boolean deleteByDocumentId(Long docId);

    /**
     * 校验向量元数据
     *
     * @param entity 向量实体
     * @param add    是否为新增
     */
    void validEmbeddingVector(EmbeddingVector entity, boolean add);

    /**
     * 获取查询包装器
     *
     * @param queryRequest 查询请求
     * @return LambdaQueryWrapper
     */
    LambdaQueryWrapper<EmbeddingVector> getQueryWrapper(EmbeddingVectorQueryRequest queryRequest);
}
