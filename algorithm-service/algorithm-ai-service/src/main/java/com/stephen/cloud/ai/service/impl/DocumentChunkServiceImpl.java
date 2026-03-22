package com.stephen.cloud.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.mapper.DocumentChunkMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.service.DocumentChunkService;
import org.springframework.stereotype.Service;

/**
 * 文档分片服务实现：持久化切片正文与序号，供 RAG 返回的 chunkId 与 ES metadata 一致。
 *
 * @author StephenQiu30
 */
@Service
public class DocumentChunkServiceImpl extends ServiceImpl<DocumentChunkMapper, DocumentChunk> implements DocumentChunkService {
}
