package com.stephen.cloud.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.convert.AiChatRecordConvert;
import com.stephen.cloud.ai.mapper.AiChatRecordMapper;
import com.stephen.cloud.ai.model.entity.AiChatRecord;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordQueryRequest;
import com.stephen.cloud.api.ai.model.vo.AiChatRecordVO;
import com.stephen.cloud.common.constants.CommonConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 对话记录服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class AiChatRecordServiceImpl extends ServiceImpl<AiChatRecordMapper, AiChatRecord>
        implements AiChatRecordService {

    @Override
    public LambdaQueryWrapper<AiChatRecord> getQueryWrapper(AiChatRecordQueryRequest aiChatRecordQueryRequest) {
        LambdaQueryWrapper<AiChatRecord> queryWrapper = new LambdaQueryWrapper<>();
        if (aiChatRecordQueryRequest == null) {
            return queryWrapper;
        }
        Long id = aiChatRecordQueryRequest.getId();
        Long userId = aiChatRecordQueryRequest.getUserId();
        String sessionId = aiChatRecordQueryRequest.getSessionId();
        String modelType = aiChatRecordQueryRequest.getModelType();
        String searchText = aiChatRecordQueryRequest.getSearchText();
        String sortField = aiChatRecordQueryRequest.getSortField();
        String sortOrder = aiChatRecordQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, AiChatRecord::getId, id)
                .eq(userId != null && userId > 0, AiChatRecord::getUserId, userId)
                .eq(StringUtils.isNotBlank(sessionId), AiChatRecord::getSessionId, sessionId)
                .eq(StringUtils.isNotBlank(modelType), AiChatRecord::getModelType, modelType);

        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw
                    .like(AiChatRecord::getMessage, searchText)
                    .or()
                    .like(AiChatRecord::getResponse, searchText));
        }

        if (StringUtils.isNotBlank(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> queryWrapper.orderBy(true, isAsc, AiChatRecord::getCreateTime);
                default -> {
                }
            }
        }
        return queryWrapper;
    }

    @Override
    public Page<AiChatRecordVO> getAiChatRecordVOPage(Page<AiChatRecord> aiChatRecordPage) {
        List<AiChatRecord> aiChatRecordList = aiChatRecordPage.getRecords();
        Page<AiChatRecordVO> aiChatRecordVOPage = new Page<>(aiChatRecordPage.getCurrent(), aiChatRecordPage.getSize(),
                aiChatRecordPage.getTotal());
        if (aiChatRecordList.isEmpty()) {
            return aiChatRecordVOPage;
        }
        List<AiChatRecordVO> aiChatRecordVOList = aiChatRecordList.stream()
                .map(AiChatRecordConvert::objToVo)
                .toList();
        aiChatRecordVOPage.setRecords(aiChatRecordVOList);
        return aiChatRecordVOPage;
    }
}
