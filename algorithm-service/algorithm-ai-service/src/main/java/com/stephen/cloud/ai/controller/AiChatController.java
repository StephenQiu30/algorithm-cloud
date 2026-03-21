package com.stephen.cloud.ai.controller;

import com.stephen.cloud.ai.service.AiChatService;
import com.stephen.cloud.api.ai.model.dto.AiChatRequest;
import com.stephen.cloud.api.ai.model.enums.AiModelTypeEnum;
import com.stephen.cloud.api.ai.model.vo.AiChatResponse;
import com.stephen.cloud.api.ai.model.vo.AiModelVO;
import com.stephen.cloud.common.cache.model.TimeModel;
import com.stephen.cloud.common.cache.utils.ratelimit.RateLimitUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.log.annotation.OperationLog;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AI 对话接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/ai")
@Slf4j
@Tag(name = "AiChatController", description = "AI 对话管理")
public class AiChatController {

    @Resource
    private AiChatService aiChatService;

    @Resource
    private RateLimitUtils rateLimitUtils;

    /**
     * AI 对话 (标准)
     * <p>
     * 发送问题并同步等待 AI 返回完整回答。
     *
     * @param aiChatRequest 对话请求
     * @param request       HTTP 请求
     * @return 包含 AI 回答的响应结果
     */
    @PostMapping("/chat")
    @OperationLog(module = "AI 管理", action = "AI 标准对话")
    @Operation(summary = "AI 对话 (标准)", description = "发送消息并等待 AI 返回完整回答")
    public BaseResponse<AiChatResponse> doAiChat(@RequestBody AiChatRequest aiChatRequest, HttpServletRequest request) {
        log.info("AI 对话请求: {}", aiChatRequest);
        ThrowUtils.throwIf(aiChatRequest == null, ErrorCode.PARAMS_ERROR);
        // 限流：每个用户每分钟最多 10 次对话
        Long userId = SecurityUtils.getLoginUserId();
        rateLimitUtils.doRateLimit("ai:chat:" + userId, new TimeModel(1L, TimeUnit.MINUTES), 10L, 1L);
        AiChatResponse response = aiChatService.chat(aiChatRequest, request);
        log.info("AI 对话响应完成: {}", response);
        return ResultUtils.success(response);
    }

    /**
     * AI 对话 (流式)
     * <p>
     * 发送问题并通过 SSE (Server-Sent Events) 逐字获取 AI 的回答。
     *
     * @param aiChatRequest 对话请求
     * @param request       HTTP 请求
     * @return 用于流式传输的 SSE 发射器
     */
    @PostMapping("/chat/stream")
    @OperationLog(module = "AI 管理", action = "AI 流式对话")
    @Operation(summary = "AI 对话 (流式)", description = "发送消息并通过 SSE 获取 AI 逐字返回的内容")
    public SseEmitter doStreamAiChat(@RequestBody AiChatRequest aiChatRequest, HttpServletRequest request) {
        log.info("AI 对话流处理已启动");
        ThrowUtils.throwIf(aiChatRequest == null, ErrorCode.PARAMS_ERROR);
        // 限流
        Long userId = SecurityUtils.getLoginUserId();
        rateLimitUtils.doRateLimit("ai:chat:" + userId, new TimeModel(1L, TimeUnit.MINUTES), 10L, 1L);
        SseEmitter emitter = new SseEmitter(60000L);
        aiChatService.streamChat(aiChatRequest, emitter, request);
        return emitter;
    }

    /**
     * 获取支持的 AI 模型列表
     * <p>
     * 获取系统当前支持的所有 AI 模型及其描述。
     *
     * @return 包含 AI 模型信息的列表
     */
    @GetMapping("/models")
    @Operation(summary = "获取支持的模型列表", description = "获取系统当前支持的所有 AI 模型及其描述")
    public BaseResponse<List<AiModelVO>> listModels() {
        List<AiModelVO> modelList = Arrays.stream(AiModelTypeEnum.values())
                .map(type -> AiModelVO.builder()
                        .name(type.getValue())
                        .description(type.getText())
                        .build())
                .collect(Collectors.toList());
        return ResultUtils.success(modelList);
    }

}
