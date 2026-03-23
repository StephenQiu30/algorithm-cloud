package com.stephen.cloud.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.mapper.DocumentChunkMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.service.DocumentChunkService;
import com.stephen.cloud.api.knowledge.model.dto.chunk.DocumentChunkQueryRequest;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import org.apache.commons.lang3.StringUtils;
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

    /**
     * 批量创建并持久化文档分片
     * <p>
     * 将解析后的长文本数组映射为具备数据库关联 ID 的分片实体，并执行批量入库优化。
     * </p>
     *
     * @param docId      所属文档的业务主键
     * @param kbId       所属知识库的业务主键
     * @param chunkTexts 被切割后的纯文本内容列表
     * @param chunkSize  系统预设的分片最大字符长度 (用于逻辑估算 Token)
     * @return 包含数据库自增 ID 的实体列表
     */
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
            // 简单 Token 估算：在未集成重型 Tokenizer 前，以字符数量作为近似参考值
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
        return this.remove(new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getDocumentId, docId));
    }

    @Override
    public void validDocumentChunk(DocumentChunk entity, boolean add) {
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (add) {
            if (entity.getDocumentId() == null || entity.getKnowledgeBaseId() == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "关联 ID 不能为空");
            }
            if (StringUtils.isBlank(entity.getContent())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容不能为空");
            }
        }
    }

    /**
     * 构造 MyBatis Plus Lambda 查询包装器
     * <p>
     * 支持多维度的分片检索：按照文档 ID、切片索引及 Token 规模进行精确匹配和灵活排序。
     * </p>
     *
     * @param queryRequest 分片查询请求 DTO
     * @return 组装完成的查询包装器
     */
    @Override
    public LambdaQueryWrapper<DocumentChunk> getQueryWrapper(DocumentChunkQueryRequest queryRequest) {
        LambdaQueryWrapper<DocumentChunk> qw = new LambdaQueryWrapper<>();
        if (queryRequest == null) {
            return qw;
        }
        Long documentId = queryRequest.getDocumentId();
        Long knowledgeBaseId = queryRequest.getKnowledgeBaseId();
        Integer chunkIndex = queryRequest.getChunkIndex();

        // 精确匹配逻辑
        qw.eq(documentId != null && documentId > 0, DocumentChunk::getDocumentId, documentId);
        qw.eq(knowledgeBaseId != null && knowledgeBaseId > 0, DocumentChunk::getKnowledgeBaseId, knowledgeBaseId);
        qw.eq(chunkIndex != null, DocumentChunk::getChunkIndex, chunkIndex);

        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();
        boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);

        // 处理排序规则
        if (SqlUtils.validSortField(sortField)) {
            switch (sortField) {
                case "createTime" -> qw.orderBy(true, isAsc, DocumentChunk::getCreateTime);
                case "chunkIndex" -> qw.orderBy(true, isAsc, DocumentChunk::getChunkIndex);
                case "tokenEstimate" -> qw.orderBy(true, isAsc, DocumentChunk::getTokenEstimate);
                default -> qw.orderByDesc(DocumentChunk::getCreateTime);
            }
        } else {
            // 默认排序：按主键升序 (即分片生成的先后顺序)
            qw.orderByAsc(DocumentChunk::getId);
        }
        return qw;
    }
}
