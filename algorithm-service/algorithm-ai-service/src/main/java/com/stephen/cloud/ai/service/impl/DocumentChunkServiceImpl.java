package com.stephen.cloud.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.mapper.DocumentChunkMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.service.DocumentChunkService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 文档分片服务实现类
 *
 * @author StephenQiu30
 */
@Service
public class DocumentChunkServiceImpl extends ServiceImpl<DocumentChunkMapper, DocumentChunk> implements DocumentChunkService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DocumentChunk> batchCreateChunks(Long docId, Long kbId, List<String> chunkTexts, int chunkSize) {
        if (chunkTexts == null || chunkTexts.isEmpty()) {
            return Collections.emptyList();
        }
        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < chunkTexts.size(); i++) {
            String content = chunkTexts.get(i);
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(docId);
            chunk.setKnowledgeBaseId(kbId);
            chunk.setChunkIndex(i);
            chunk.setContent(content);
            // Token 估算：简单以字符长度为准，受限于配置的 chunkSize
            chunk.setTokenEstimate(Math.min(content.length(), chunkSize));
            chunks.add(chunk);
        }
        this.saveBatch(chunks);
        return chunks;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByDocumentId(Long docId) {
        if (docId == null) {
            return false;
        }
        LambdaQueryWrapper<DocumentChunk> qw = new LambdaQueryWrapper<>();
        qw.eq(DocumentChunk::getDocumentId, docId);
        return this.remove(qw);
    }
}
