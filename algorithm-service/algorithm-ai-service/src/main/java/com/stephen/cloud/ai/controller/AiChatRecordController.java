package com.stephen.cloud.ai.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.model.entity.AiChatRecord;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordQueryRequest;
import com.stephen.cloud.api.ai.model.vo.AiChatRecordVO;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.DeleteRequest;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 对话记录接口
 * <p>
 * 管理用户的对话历史，支持分页查询、本人记录删除及管理员全局管理。
 * </p>
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/chat/record")
@Slf4j
@Tag(name = "AiChatRecordController", description = "AI 对话记录管理接口")
public class AiChatRecordController {

    @Resource
    private AiChatRecordService aiChatRecordService;

    /**
     * 删除对话记录 (仅限本人或管理员)
     *
     * @param deleteRequest 包含记录 ID 的请求
     * @param request       请求对象
     * @return 是否成功
     */
    @PostMapping("/delete")
    @Operation(summary = "删除对话记录")
    @OperationLog(module = "AI 对话模块", action = "删除对话记录")
    public BaseResponse<Boolean> deleteAiChatRecord(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = deleteRequest.getId();
        AiChatRecord oldRecord = aiChatRecordService.getById(id);
        if (oldRecord == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        Long userId = SecurityUtils.getLoginUserId();
        // 鉴权：仅本人或管理员可删
        if (!oldRecord.getUserId().equals(userId) && !SecurityUtils.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = aiChatRecordService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 分页查询我的对话历史
     *
     * @param queryRequest 分页查询请求
     * @param request      请求对象
     * @return 对话记录分页
     */
    @PostMapping("/my/list/page/vo")
    @Operation(summary = "分页获取我的对话历史")
    public BaseResponse<Page<AiChatRecordVO>> listMyAiChatRecordVOByPage(@RequestBody AiChatRecordQueryRequest queryRequest, HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        queryRequest.setUserId(SecurityUtils.getLoginUserId());
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        // 防御式编程：限制查询页大小
        if (size > 50) {
            size = 50;
        }
        Page<AiChatRecord> aiChatRecordPage = aiChatRecordService.page(new Page<>(current, size),
                aiChatRecordService.getQueryWrapper(queryRequest));
        return ResultUtils.success(aiChatRecordService.getAiChatRecordVOPage(aiChatRecordPage, request));
    }

    /**
     * 分页查询对话记录 (管理员)
     *
     * @param queryRequest 分页查询请求
     * @param request      请求对象
     * @return 对话记录分页
     */
    @PostMapping("/list/page/vo")
    @Operation(summary = "管理员分页获取对话记录")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AiChatRecordVO>> listAiChatRecordVOByPage(@RequestBody AiChatRecordQueryRequest queryRequest, HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<AiChatRecord> aiChatRecordPage = aiChatRecordService.page(new Page<>(current, size),
                aiChatRecordService.getQueryWrapper(queryRequest));
        return ResultUtils.success(aiChatRecordService.getAiChatRecordVOPage(aiChatRecordPage, request));
    }
}
