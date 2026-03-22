package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.AiChatRecord;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordDTO;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordQueryRequest;
import com.stephen.cloud.api.ai.model.vo.AiChatRecordVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * AI 对话记录服务
 * <p>
 * 管理历史对话数据的生命周期，提供多维度查询及向前端展示的视图封装。
 * </p>
 *
 * @author StephenQiu30
 */
public interface AiChatRecordService extends IService<AiChatRecord> {

    /**
     * 异步持久化对话记录
     *
     * @param aiChatRecordDTO 记录传输对象
     */
    void saveAiChatRecordAsync(AiChatRecordDTO aiChatRecordDTO);

    /**
     * 校验对话记录
     *
     * @param aiChatRecord 对话记录实体
     * @param add          是否为新增操作
     */
    void validAiChatRecord(AiChatRecord aiChatRecord, boolean add);

    /**
     * 构建查询条件封装
     *
     * @param aiChatRecordQueryRequest 查询请求对象
     * @return LambdaQueryWrapper
     */
    LambdaQueryWrapper<AiChatRecord> getQueryWrapper(AiChatRecordQueryRequest aiChatRecordQueryRequest);

    /**
     * 获取对话记录视图封装 (包含用户信息)
     *
     * @param aiChatRecord 对话记录实体
     * @param request      HTTP 请求
     * @return 对话记录视图
     */
    AiChatRecordVO getAiChatRecordVO(AiChatRecord aiChatRecord, HttpServletRequest request);

    /**
     * 分页获取对话记录视图封装
     *
     * @param aiChatRecordPage 对话记录分页
     * @param request          HTTP 请求
     * @return 对话记录视图分页
     */
    Page<AiChatRecordVO> getAiChatRecordVOPage(Page<AiChatRecord> aiChatRecordPage, HttpServletRequest request);
}
