package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.api.ai.model.dto.chunk.ChunkQueryRequest;
import com.stephen.cloud.api.ai.model.dto.chunk.ChunkSearchRequest;
import com.stephen.cloud.api.ai.model.vo.ChunkVO;

import java.util.List;

public interface ChunkService extends IService<DocumentChunk> {

    /**
     * 获取查询条件
     */
    LambdaQueryWrapper<DocumentChunk> getQueryWrapper(ChunkQueryRequest queryRequest);

    /**
     * 分页查询文档分片列表
     */
    Page<ChunkVO> getChunkVOPage(Page<DocumentChunk> page);

    /**
     * 对象转 VO
     */
    ChunkVO getChunkVO(DocumentChunk chunk);

    /**
     * 基于内容进行混合检索（向量 + 关键词 + RRF融合）
     */
    List<ChunkVO> searchChunks(ChunkSearchRequest request);
}
