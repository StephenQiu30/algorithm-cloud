package com.stephen.cloud.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.mapper.EmbeddingVectorMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.model.entity.EmbeddingVector;
import com.stephen.cloud.ai.service.EmbeddingVectorService;
import com.stephen.cloud.api.knowledge.model.dto.vector.EmbeddingVectorQueryRequest;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    /**
     * 批量保存向量化审计日志
     * <p>
     * 记录每个切片被向量化后的元数据信息，如使用的模型、维度以及在 Elasticsearch 中的文档 ID。
     * </p>
     *
     * @param chunks    文档分片实体列表 (需包含数据库 ID)
     * @param model     本次向量化使用的模型标识 (如 "text-embedding-v1")
     * @param dimension 向量维度 (通常为 1024, 768 等)
     * @param esDocIds  在向量存储引擎 (ES) 中对应的文档唯一标识列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSaveAuditLogs(List<DocumentChunk> chunks, String model, int dimension, List<String> esDocIds) {
        if (chunks == null || esDocIds == null || chunks.size() != esDocIds.size()) {
            log.warn("向量审计日志参数不匹配: chunks={}, esDocs={}", 
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
        log.info("已批量持久化 {} 条向量化审计记录", vectors.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByDocumentId(Long docId) {
        if (docId == null) {
            return false;
        }
        // 基于 document_chunk 表关联删除向量
        return this.remove(new LambdaQueryWrapper<EmbeddingVector>()
                .inSql(EmbeddingVector::getChunkId, "SELECT id FROM document_chunk WHERE document_id = " + docId));
    }

    @Override
    public void validEmbeddingVector(EmbeddingVector entity, boolean add) {
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (add) {
            if (entity.getChunkId() == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "关联分片 ID 不能为空");
            }
            if (StringUtils.isBlank(entity.getEmbeddingModel())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "模型名称不能为空");
            }
        }
    }

    /**
     * 构造 MyBatis Plus Lambda 查询包装器
     * <p>
     * 支持多维度的向量审计记录检索，包含切片 ID、模型标识的匹配及时间维度的排序。
     * </p>
     *
     * @param queryRequest 审计查询请求对象
     * @return 组装完成的查询包装器
     */
    @Override
    public LambdaQueryWrapper<EmbeddingVector> getQueryWrapper(EmbeddingVectorQueryRequest queryRequest) {
        LambdaQueryWrapper<EmbeddingVector> qw = new LambdaQueryWrapper<>();
        if (queryRequest == null) {
            return qw;
        }
        Long id = queryRequest.getId();
        Long chunkId = queryRequest.getChunkId();
        String embeddingModel = queryRequest.getEmbeddingModel();

        // 精确匹配
        qw.eq(id != null && id > 0, EmbeddingVector::getId, id);
        qw.eq(chunkId != null && chunkId > 0, EmbeddingVector::getChunkId, chunkId);
        qw.like(StringUtils.isNotBlank(embeddingModel), EmbeddingVector::getEmbeddingModel, embeddingModel);

        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();
        boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);

        // 排序规则设置
        if (SqlUtils.validSortField(sortField)) {
            switch (sortField) {
                case "createTime" -> qw.orderBy(true, isAsc, EmbeddingVector::getCreateTime);
                default -> qw.orderByDesc(EmbeddingVector::getCreateTime);
            }
        } else {
            // 默认排序：按主键升序
            qw.orderByAsc(EmbeddingVector::getId);
        }
        return qw;
    }
}
