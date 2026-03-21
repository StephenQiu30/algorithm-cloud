package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.AiChatRecord;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordQueryRequest;
import com.stephen.cloud.api.ai.model.vo.AiChatRecordVO;

/**
 * AI 对话记录服务
 *
 * @author StephenQiu30
 */
public interface AiChatRecordService extends IService<AiChatRecord> {

    /**
     * 获取查询条件
     *
     * @param aiChatRecordQueryRequest 查询请求
     * @return 查询条件
     */
    LambdaQueryWrapper<AiChatRecord> getQueryWrapper(AiChatRecordQueryRequest aiChatRecordQueryRequest);

    /**
     * 获取分页记录视图
     *
     * @param aiChatRecordPage 分页记录
     * @return 分页视图
     */
    Page<AiChatRecordVO> getAiChatRecordVOPage(Page<AiChatRecord> aiChatRecordPage);
}
