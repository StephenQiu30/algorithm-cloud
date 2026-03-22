package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.DocumentChunk;

/**
 * 文档分片表业务服务：持久化切片正文与序号，主键 {@code id} 写入 ES metadata 的 {@code chunkId}，供 RAG 引用展示。
 *
 * @author StephenQiu30
 */
public interface DocumentChunkService extends IService<DocumentChunk> {
}
