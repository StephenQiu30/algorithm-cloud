package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.api.ai.model.dto.chunk.ChunkQueryRequest;
import com.stephen.cloud.api.ai.model.dto.chunk.ChunkSearchRequest;
import com.stephen.cloud.api.ai.model.vo.ChunkVO;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncTypeEnum;
import jakarta.servlet.http.HttpServletRequest;


import java.util.Date;
import java.util.List;

/**
 * 文档分片服务
 *
 * @author StephenQiu30
 */
public interface ChunkService extends IService<DocumentChunk> {

    /**
     * 同步单个分片到 ES
     *
     * @param chunkId 分片 ID
     */
    void syncToEs(Long chunkId);

    /**
     * 同步分片数据到 ES
     *
     * @param syncType      同步方式（全量或增量）
     * @param minUpdateTime 最小更新时间 (仅在增量同步时生效)
     */
    void syncToEs(EsSyncTypeEnum syncType, Date minUpdateTime);

    /**
     * 获取查询条件
     */
    LambdaQueryWrapper<DocumentChunk> getQueryWrapper(ChunkQueryRequest queryRequest);

    /**
     * 分页查询文档分片列表
     */
    Page<ChunkVO> getChunkVOPage(Page<DocumentChunk> page, HttpServletRequest request);

    /**
     * 对象转 VO
     */
    ChunkVO getChunkVO(DocumentChunk chunk, HttpServletRequest request);

    /**
     * 基于内容进行混合检索（向量 + 关键词 + RRF融合）
     */
    List<ChunkVO> searchChunks(ChunkSearchRequest request);
}
