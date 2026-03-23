package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.api.knowledge.model.dto.chunk.DocumentChunkQueryRequest;

/**
 * 文档分片服务接口
 *
 * @author StephenQiu30
 */
public interface DocumentChunkService extends IService<DocumentChunk> {

    /**
     * 获取查询条件
     */
    LambdaQueryWrapper<DocumentChunk> getQueryWrapper(DocumentChunkQueryRequest queryRequest);

    /**
     * 按文档 ID 删除分片
     */
    boolean deleteByDocumentId(Long docId);
}
