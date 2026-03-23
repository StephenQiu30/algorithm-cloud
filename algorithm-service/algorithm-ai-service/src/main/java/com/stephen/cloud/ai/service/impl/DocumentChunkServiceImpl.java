package com.stephen.cloud.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.mapper.DocumentChunkMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.service.DocumentChunkService;
import com.stephen.cloud.api.knowledge.model.dto.chunk.DocumentChunkQueryRequest;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 文档分片服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class DocumentChunkServiceImpl extends ServiceImpl<DocumentChunkMapper, DocumentChunk> implements DocumentChunkService {

    @Override
    public LambdaQueryWrapper<DocumentChunk> getQueryWrapper(DocumentChunkQueryRequest queryRequest) {
        LambdaQueryWrapper<DocumentChunk> qw = new LambdaQueryWrapper<>();
        if (queryRequest == null) return qw;
        qw.eq(queryRequest.getDocumentId() != null, DocumentChunk::getDocumentId, queryRequest.getDocumentId());
        qw.eq(queryRequest.getKnowledgeBaseId() != null, DocumentChunk::getKnowledgeBaseId, queryRequest.getKnowledgeBaseId());
        qw.eq(queryRequest.getChunkIndex() != null, DocumentChunk::getChunkIndex, queryRequest.getChunkIndex());
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();
        boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
        if (SqlUtils.validSortField(sortField)) {
            switch (sortField) {
                case "createTime" -> qw.orderBy(true, isAsc, DocumentChunk::getCreateTime);
                case "chunkIndex" -> qw.orderBy(true, isAsc, DocumentChunk::getChunkIndex);
                case "tokenEstimate" -> qw.orderBy(true, isAsc, DocumentChunk::getTokenEstimate);
                default -> qw.orderByDesc(DocumentChunk::getCreateTime);
            }
        } else {
            qw.orderByDesc(DocumentChunk::getCreateTime);
        }
        return qw;
    }

    @Override
    public boolean deleteByDocumentId(Long docId) {
        if (docId == null) return false;
        return this.remove(new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getDocumentId, docId));
    }
}
