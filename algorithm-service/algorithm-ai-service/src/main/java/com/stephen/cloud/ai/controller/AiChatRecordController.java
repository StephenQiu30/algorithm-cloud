package com.stephen.cloud.ai.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.model.entity.AiChatRecord;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.ai.service.RagService;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordQueryRequest;
import com.stephen.cloud.api.ai.model.vo.AiChatRecordVO;
import com.stephen.cloud.api.knowledge.model.dto.rag.RagChatRequest;
import com.stephen.cloud.api.knowledge.model.vo.RagChatResponseVO;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.*;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * AI 对话记录管理接口
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

    @Resource
    private RagService ragService;

    /**
     * 发起 RAG 问答请求
     *
     * @param request 问答请求配置
     * @return 问答结果包装类
     */
    @PostMapping("/chat")
    @Operation(summary = "发起 RAG 问答", description = "核心接口：基于指定知识库执行检索增强生成的同步问答。")
    @OperationLog(module = "AI 对话模块", action = "发起 RAG 问答")
    public BaseResponse<RagChatResponseVO> ragChat(@RequestBody RagChatRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = SecurityUtils.getLoginUserId();
        log.info("[RAG问答] 收到问答请求: userId={}, kbId={}", userId, request.getKnowledgeBaseId());
        RagChatResponseVO response = ragService.ragChat(request, userId);
        return ResultUtils.success(response);
    }

    /**
     * 发起流式 RAG 问答请求
     *
     * @param request 问答请求配置
     * @return 响应式流数据
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式 RAG 问答", description = "基于 SSE 协议的检索增强生成流式输出。")
    @OperationLog(module = "AI 对话模块", action = "流式 RAG 问答")
    public Flux<RagChatResponseVO> streamRagChat(@RequestBody RagChatRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = SecurityUtils.getLoginUserId();
        log.info("[RAG流式] 收到流式问答请求: userId={}, kbId={}", userId, request.getKnowledgeBaseId());
        return ragService.streamRagChat(request, userId);
    }

    /**
     * 分页查询当前用户的对话历史
     *
     * @param queryRequest 分页查询请求
     * @param request      请求对象
     * @return 对话记录分页
     */
    @PostMapping("/my/list/page/vo")
    @Operation(summary = "分页获取我的对话历史", description = "获取当前登录用户的历史对话列表。")
    public BaseResponse<Page<AiChatRecordVO>> listMyAiChatRecordVOByPage(@RequestBody AiChatRecordQueryRequest queryRequest, HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        queryRequest.setUserId(SecurityUtils.getLoginUserId());
        long current = queryRequest.getCurrent();
        long size = Math.min(queryRequest.getPageSize(), 50);
        Page<AiChatRecord> aiChatRecordPage = aiChatRecordService.page(new Page<>(current, size),
                aiChatRecordService.getQueryWrapper(queryRequest));
        return ResultUtils.success(aiChatRecordService.getAiChatRecordVOPage(aiChatRecordPage, request));
    }

    /**
     * 根据 ID 获取对话记录详情
     *
     * @param id      记录 ID
     * @param request 请求对象
     * @return 对话记录 VO
     */
    @GetMapping("/get/vo")
    @Operation(summary = "获取对话记录详情", description = "根据 ID 获取单条对话记录的详细信息。")
    public BaseResponse<AiChatRecordVO> getAiChatRecordVOById(@RequestParam("id") long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        AiChatRecord aiChatRecord = aiChatRecordService.getById(id);
        ThrowUtils.throwIf(aiChatRecord == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(aiChatRecordService.getAiChatRecordVO(aiChatRecord, request));
    }

    /**
     * 删除对话记录（仅限本人或管理员）
     *
     * @param deleteRequest 包含记录 ID 的请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    @Operation(summary = "删除对话记录", description = "删除指定的对话记录，仅本人或管理员可操作。")
    @OperationLog(module = "AI 对话模块", action = "删除对话记录")
    public BaseResponse<Boolean> deleteAiChatRecord(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = deleteRequest.getId();
        AiChatRecord oldRecord = aiChatRecordService.getById(id);
        if (oldRecord == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        Long userId = SecurityUtils.getLoginUserId();
        if (!oldRecord.getUserId().equals(userId) && !SecurityUtils.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = aiChatRecordService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 分页查询所有对话记录（管理员）
     *
     * @param queryRequest 分页查询请求
     * @param request      请求对象
     * @return 对话记录分页
     */
    @PostMapping("/list/page/vo")
    @Operation(summary = "管理员分页获取对话记录", description = "管理员视角查看所有用户的对话记录。")
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
