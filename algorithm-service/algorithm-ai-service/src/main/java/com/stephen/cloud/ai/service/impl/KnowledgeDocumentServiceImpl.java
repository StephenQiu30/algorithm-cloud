package com.stephen.cloud.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.convert.KnowledgeDocumentConvert;
import com.stephen.cloud.ai.mapper.KnowledgeDocumentMapper;
import com.stephen.cloud.ai.model.entity.KnowledgeDocument;
import com.stephen.cloud.ai.service.KnowledgeDocumentService;
import com.stephen.cloud.api.knowledge.model.dto.knowledgedocument.KnowledgeDocumentQueryRequest;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeDocumentVO;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识文档服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument> implements KnowledgeDocumentService {

    @Override
    public void validKnowledgeDocument(KnowledgeDocument entity, boolean add) {
        if (entity == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(entity.getOriginalName()), ErrorCode.PARAMS_ERROR, "文件名不能为空");
            ThrowUtils.throwIf(entity.getKnowledgeBaseId() == null, ErrorCode.PARAMS_ERROR, "知识库 ID 不能为空");
        }
    }

    @Override
    public LambdaQueryWrapper<KnowledgeDocument> getQueryWrapper(KnowledgeDocumentQueryRequest queryRequest) {
        LambdaQueryWrapper<KnowledgeDocument> qw = new LambdaQueryWrapper<>();
        if (queryRequest == null) return qw;
        qw.eq(queryRequest.getKnowledgeBaseId() != null, KnowledgeDocument::getKnowledgeBaseId, queryRequest.getKnowledgeBaseId());
        qw.like(StringUtils.isNotBlank(queryRequest.getOriginalName()), KnowledgeDocument::getOriginalName, queryRequest.getOriginalName());
        qw.eq(queryRequest.getParseStatus() != null, KnowledgeDocument::getParseStatus, queryRequest.getParseStatus());
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();
        if (SqlUtils.validSortField(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> qw.orderBy(true, isAsc, KnowledgeDocument::getCreateTime);
                case "updateTime" -> qw.orderBy(true, isAsc, KnowledgeDocument::getUpdateTime);
                default -> qw.orderByDesc(KnowledgeDocument::getUpdateTime);
            }
        } else {
            qw.orderByDesc(KnowledgeDocument::getUpdateTime);
        }
        return qw;
    }

    @Override
    public KnowledgeDocumentVO getKnowledgeDocumentVO(KnowledgeDocument entity) {
        if (entity == null) return null;
        return KnowledgeDocumentConvert.INSTANCE.entityToDocumentVo(entity);
    }

    @Override
    public Page<KnowledgeDocumentVO> getKnowledgeDocumentVOPage(Page<KnowledgeDocument> page) {
        List<KnowledgeDocument> records = page.getRecords();
        Page<KnowledgeDocumentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (records.isEmpty()) return voPage;
        voPage.setRecords(records.stream().map(KnowledgeDocumentConvert.INSTANCE::entityToDocumentVo).collect(Collectors.toList()));
        return voPage;
    }
}
