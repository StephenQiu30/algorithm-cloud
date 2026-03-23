package com.stephen.cloud.ai.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.convert.AiChatRecordConvert;
import com.stephen.cloud.ai.model.entity.AiChatRecord;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.ai.service.RagService;
import com.stephen.cloud.api.knowledge.model.dto.rag.RagChatRequest;
import com.stephen.cloud.api.knowledge.model.vo.RagChatResponseVO;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordAddRequest;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordEditRequest;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordQueryRequest;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordUpdateRequest;
import com.stephen.cloud.api.ai.model.vo.AiChatRecordVO;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.DeleteRequest;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.log.annotation.OperationLog;
import com.stephen.cloud.common.common.ThrowUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
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
     * 创建对话记录
     *
     * @param addRequest 创建请求
     * @param request    请求对象
     * @return 记录 ID
     */
    @PostMapping("/add")
    @Operation(summary = "创建对话记录", description = "用户发起新的对话并保存初始信息。")
    @OperationLog(module = "AI 对话模块", action = "创建对话记录")
    public BaseResponse<Long> addAiChatRecord(@RequestBody AiChatRecordAddRequest addRequest) {
        if (addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        AiChatRecord aiChatRecord = AiChatRecordConvert.INSTANCE.addRequestToObj(addRequest);
        aiChatRecord.setUserId(SecurityUtils.getLoginUserId());
        aiChatRecordService.validAiChatRecord(aiChatRecord, true);
        boolean result = aiChatRecordService.save(aiChatRecord);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(aiChatRecord.getId());
    }

    /**
     * 更新对话记录 (管理员)
     *
     * @param updateRequest 更新请求
     * @param request       请求对象
     * @return 是否成功
     */
    @PostMapping("/update")
    @Operation(summary = "更新对话记录(管理员)", description = "系统管理员全量更新指定对话记录信息。")
    @OperationLog(module = "AI 对话模块", action = "更新对话记录(管理员)")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAiChatRecord(@RequestBody AiChatRecordUpdateRequest updateRequest) {
        if (updateRequest == null || updateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        AiChatRecord aiChatRecord = AiChatRecordConvert.INSTANCE.updateRequestToObj(updateRequest);
        aiChatRecordService.validAiChatRecord(aiChatRecord, false);
        long id = updateRequest.getId();
        AiChatRecord oldRecord = aiChatRecordService.getById(id);
        ThrowUtils.throwIf(oldRecord == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = aiChatRecordService.updateById(aiChatRecord);
        return ResultUtils.success(result);
    }

    /**
     * 编辑对话记录 (用户本人)
     *
     * @param editRequest 编辑请求
     * @param request     请求对象
     * @return 是否成功
     */
    @PostMapping("/edit")
    @Operation(summary = "编辑对话记录", description = "编辑对话信息，仅本人可操作。")
    @OperationLog(module = "AI 对话模块", action = "编辑对话记录")
    public BaseResponse<Boolean> editAiChatRecord(@RequestBody AiChatRecordEditRequest editRequest) {
        if (editRequest == null || editRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        AiChatRecord aiChatRecord = AiChatRecordConvert.INSTANCE.editRequestToObj(editRequest);
        aiChatRecordService.validAiChatRecord(aiChatRecord, false);
        long id = editRequest.getId();
        AiChatRecord oldRecord = aiChatRecordService.getById(id);
        if (oldRecord == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可编辑
        if (!oldRecord.getUserId().equals(SecurityUtils.getLoginUserId()) && !SecurityUtils.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = aiChatRecordService.updateById(aiChatRecord);
        return ResultUtils.success(result);
    }

    /**
     * 删除对话记录 (仅限本人或管理员)
     *
     * @param deleteRequest 包含记录 ID 的请求
     * @param request       请求对象
     * @return 是否成功
     */
    @PostMapping("/delete")
    @Operation(summary = "删除对话记录", description = "物理删除单条指定的对话记录，仅本人或管理员可操作。")
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
        // 鉴权：仅本人或管理员可删
        if (!oldRecord.getUserId().equals(userId) && !SecurityUtils.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = aiChatRecordService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 根据 ID 获取对话记录 (脱敏)
     *
     * @param id 记录 ID
     * @param request 请求对象
     * @return 对话记录 VO
     */
    @GetMapping("/get/vo")
    @Operation(summary = "获取对话记录详情", description = "根据 ID 获取单条对话记录的脱敏信息。")
    public BaseResponse<AiChatRecordVO> getAiChatRecordVOById(@RequestParam("id") long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        AiChatRecord aiChatRecord = aiChatRecordService.getById(id);
        ThrowUtils.throwIf(aiChatRecord == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(aiChatRecordService.getAiChatRecordVO(aiChatRecord, request));
    }

    /**
     * 分页查询当前登录用户的对话历史
     *
     * @param queryRequest 分页查询请求
     * @param request      请求对象
     * @return 对话记录分页
     */
    @PostMapping("/my/list/page/vo")
    @Operation(summary = "分页获取我的对话历史", description = "获取当前登录用户的历史对话列表，支持搜索。")
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
     * 分页查询所有对话记录 (管理员)
     *
     * @param queryRequest 分页查询请求
     * @param request      请求对象
     * @return 对话记录分页
     */
    @PostMapping("/list/page/vo")
    @Operation(summary = "管理员分页获取对话记录", description = "管理员视角的分页查询。")
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
        log.info("[AiChatRecordController] Combined RAG chat requested by userId={}", userId);
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
        log.info("[AiChatRecordController] Streaming RAG chat requested by userId={}", userId);
        return ragService.streamRagChat(request, userId);
    }
}
