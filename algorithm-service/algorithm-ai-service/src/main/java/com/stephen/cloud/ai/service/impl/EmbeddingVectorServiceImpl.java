package com.stephen.cloud.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.mapper.EmbeddingVectorMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.model.entity.EmbeddingVector;
import com.stephen.cloud.ai.service.EmbeddingVectorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 向量元数据服务实现类
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class EmbeddingVectorServiceImpl extends ServiceImpl<EmbeddingVectorMapper, EmbeddingVector> implements EmbeddingVectorService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSaveAuditLogs(List<DocumentChunk> chunks, String model, int dimension, List<String> esDocIds) {
        if (chunks == null || esDocIds == null || chunks.size() != esDocIds.size()) {
            log.warn("Parameter mismatch for audit logging: chunks={}, esDocs={}", 
                chunks != null ? chunks.size() : 0, esDocIds != null ? esDocIds.size() : 0);
            return;
        }

        List<EmbeddingVector> vectors = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            EmbeddingVector vector = new EmbeddingVector();
            vector.setChunkId(chunk.getId());
            vector.setEmbeddingModel(model);
            vector.setDimension(dimension);
            vector.setEsDocId(esDocIds.get(i));
            vectors.add(vector);
        }
        this.saveBatch(vectors);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByDocumentId(Long docId) {
        if (docId == null) {
            return;
        }
        // 基于 documento_chunk 表关联删除审计日志
        this.remove(new LambdaQueryWrapper<EmbeddingVector>()
                .inSql(EmbeddingVector::getChunkId, "SELECT id FROM document_chunk WHERE document_id = " + docId));
    }
}
