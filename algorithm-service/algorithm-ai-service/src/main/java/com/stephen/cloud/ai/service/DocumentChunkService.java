package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.api.knowledge.model.dto.chunk.DocumentChunkQueryRequest;

import java.util.List;

/**
 * 文档分片表业务服务：持久化切片正文与序号，并在 RAG 时供搜索增强使用。
 *
 * @author StephenQiu30
 */
public interface DocumentChunkService extends IService<DocumentChunk> {

    /**
     * 批量创建并保存文档分片。
     *
     * @param docId      所属文档 ID
     * @param kbId       所属知识库 ID
     * @param chunkTexts 文本分片列表
     * @param chunkSize  分片阈值（用于 Token 估算）
     * @return 已保存的实体列表（带 ID）
     */
    List<DocumentChunk> batchCreateChunks(Long docId, Long kbId, List<String> chunkTexts, int chunkSize);

    /**
     * 根据文档 ID 清理所有分片。
     *
     * @param docId 文档 ID
     * @return 是否清理成功
     */
    boolean deleteByDocumentId(Long docId);

    /**
     * 校验分片
     *
     * @param entity 分片实体
     * @param add    是否为新增
     */
    void validDocumentChunk(DocumentChunk entity, boolean add);

    /**
     * 获取查询包装器
     *
     * @param queryRequest 查询请求
     * @return LambdaQueryWrapper
     */
    LambdaQueryWrapper<DocumentChunk> getQueryWrapper(DocumentChunkQueryRequest queryRequest);
}
