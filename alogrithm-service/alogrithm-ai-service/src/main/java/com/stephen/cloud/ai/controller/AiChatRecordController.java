package com.stephen.cloud.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.model.entity.AiChatRecord;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordQueryRequest;
import com.stephen.cloud.api.ai.model.vo.AiChatRecordVO;
import com.stephen.cloud.api.ai.model.vo.AiChatRecordVO;
import com.stephen.cloud.common.common.*;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.log.annotation.OperationLog;
import org.springframework.web.bind.annotation.*;

/**
 * AI 对话记录接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/ai/record")
@Slf4j
@Tag(name = "AiChatRecordController", description = "AI 对话记录管理接口")
public class AiChatRecordController {

    @Resource
    private AiChatRecordService aiChatRecordService;

    /**
     * 分页获取对话记录列表 (仅管理员)
     * <p>
     * 获取完整字段的对话记录列表，主要用于管理后台或数据同步。
     *
     * @param aiChatRecordQueryRequest 查询请求
     * @param request                  HTTP 请求
     * @return 对话记录分页列表
     */
    @PostMapping("/list/page")
    @Operation(summary = "分页获取对话记录列表 (管理员)", description = "获取完整字段的对话记录列表，仅管理员可用")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AiChatRecord>> listAiChatRecordByPage(@RequestBody AiChatRecordQueryRequest aiChatRecordQueryRequest,
            HttpServletRequest request) {
        log.info("分页获取 AI 对话记录列表请求: {}", aiChatRecordQueryRequest);
        ThrowUtils.throwIf(aiChatRecordQueryRequest == null, ErrorCode.PARAMS_ERROR);
        if (aiChatRecordQueryRequest == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        long current = aiChatRecordQueryRequest.getCurrent();
        long size = aiChatRecordQueryRequest.getPageSize();
        Page<AiChatRecord> aiChatRecordPage = aiChatRecordService.page(new Page<>(current, size),
                aiChatRecordService.getQueryWrapper(aiChatRecordQueryRequest));
        log.info("分页获取 AI 对话记录列表成功, 总条数: {}", aiChatRecordPage.getTotal());
        return ResultUtils.success(aiChatRecordPage);
    }

    /**
     * 分页获取对话记录列表 (VO 版，管理员)
     * <p>
     * 获取脱敏后的对话记录列表。
     *
     * @param aiChatRecordQueryRequest 查询请求
     * @param request                  HTTP 请求
     * @return 对话记录 VO 分页列表
     */
    @PostMapping("/list/page/vo")
    @Operation(summary = "分页获取对话记录列表 (VO 版)", description = "分页获取 AI 对话记录脱敏信息列表，主要由管理员用于管理后台")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AiChatRecordVO>> listAiChatRecordVOByPage(@RequestBody AiChatRecordQueryRequest aiChatRecordQueryRequest,
            HttpServletRequest request) {
        log.info("分页获取 AI 对话记录 VO 列表请求: {}", aiChatRecordQueryRequest);
        ThrowUtils.throwIf(aiChatRecordQueryRequest == null, ErrorCode.PARAMS_ERROR);
        if (aiChatRecordQueryRequest == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        long current = aiChatRecordQueryRequest.getCurrent();
        long size = aiChatRecordQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<AiChatRecord> aiChatRecordPage = aiChatRecordService.page(new Page<>(current, size),
                aiChatRecordService.getQueryWrapper(aiChatRecordQueryRequest));
        Page<AiChatRecordVO> voPage = aiChatRecordService.getAiChatRecordVOPage(aiChatRecordPage);
        log.info("分页获取 AI 对话记录 VO 列表成功, 总条数: {}", voPage.getTotal());
        return ResultUtils.success(voPage);
    }

    /**
     * 分页获取我的对话记录
     * <p>
     * 获取当前登录用户的历史 AI 对话记录，支持分页。
     *
     * @param aiChatRecordQueryRequest 查询请求
     * @param request                  HTTP 请求
     * @return 对话记录分页
     */
    @PostMapping("/my/list/page/vo")
    @Operation(summary = "分页获取我的对话记录", description = "获取当前登录用户的历史 AI 对话记录，支持分页")
    public BaseResponse<Page<AiChatRecordVO>> listMyAiChatRecordVOByPage(
            @RequestBody AiChatRecordQueryRequest aiChatRecordQueryRequest,
            HttpServletRequest request) {
        log.info("分页获取 AI 对话记录请求: {}", aiChatRecordQueryRequest);
        ThrowUtils.throwIf(aiChatRecordQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        long current = aiChatRecordQueryRequest.getCurrent();
        long size = aiChatRecordQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        LambdaQueryWrapper<AiChatRecord> queryWrapper = aiChatRecordService.getQueryWrapper(aiChatRecordQueryRequest);
        queryWrapper.eq(AiChatRecord::getUserId, userId);
        Page<AiChatRecord> aiChatRecordPage = aiChatRecordService.page(new Page<>(current, size), queryWrapper);
        Page<AiChatRecordVO> voPage = aiChatRecordService.getAiChatRecordVOPage(aiChatRecordPage);
        log.info("分页获取 AI 对话记录成功, 总条数: {}", voPage.getTotal());
        return ResultUtils.success(voPage);
    }

    /**
     * 删除对话记录
     * <p>
     * 根据 ID 删除指定的对话记录，仅支持删除本人的记录。
     *
     * @param deleteRequest 删除请求
     * @param request       HTTP 请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    @Operation(summary = "删除对话记录", description = "删除指定的 AI 对话记录，仅限管理员或记录所有者")
    @OperationLog(module = "AI 管理", action = "删除 AI 对话记录")
    public BaseResponse<Boolean> deleteAiChatRecord(@RequestBody DeleteRequest deleteRequest,
            HttpServletRequest request) {
        log.info("删除 AI 对话记录请求: {}", deleteRequest);
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        AiChatRecord oldAiChatRecord = aiChatRecordService.getById(id);
        ThrowUtils.throwIf(oldAiChatRecord == null, ErrorCode.NOT_FOUND_ERROR);

        // 仅本人可删除
        Long userId = SecurityUtils.getLoginUserId();
        if (oldAiChatRecord != null) {
            ThrowUtils.throwIf(!oldAiChatRecord.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR);
        }

        boolean result = aiChatRecordService.removeById(id);
        log.info("删除 AI 对话记录结果: {}, id: {}", result, id);
        return ResultUtils.success(result);
    }

}
