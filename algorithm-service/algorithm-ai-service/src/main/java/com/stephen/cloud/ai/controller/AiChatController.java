package com.stephen.cloud.ai.controller;

import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.service.AiChatService;
import com.stephen.cloud.api.ai.model.dto.AiChatRequest;
import com.stephen.cloud.api.ai.model.vo.AiChatResponse;
import com.stephen.cloud.api.ai.model.vo.AiModelVO;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI 对话交互接口
 * <p>
 * 支持标准 HTTP 对话、实时 SSE 流式对话及可用模型能力列表。
 * </p>
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/chat")
@Slf4j
@Tag(name = "AiChatController", description = "AI 核心对话交互接口")
public class AiChatController {

    @Resource
    private AiChatService aiChatService;

    @Resource
    private KnowledgeProperties knowledgeProperties;

    /**
     * 标准同步对话 (等待 AI 全部回复完成后返回)
     *
     * @param aiChatRequest 对话请求
     * @param request       请求对象
     * @return AI 完整回答
     */
    @PostMapping("/doChat")
    @Operation(summary = "标准同步 AI 对话")
    @OperationLog(module = "AI 对话模块", action = "标准 AI 对话")
    public BaseResponse<AiChatResponse> doChat(@RequestBody AiChatRequest aiChatRequest, HttpServletRequest request) {
        return ResultUtils.success(aiChatService.chat(aiChatRequest, request));
    }

    /**
     * 实时流式对话 (基于 SSE 实现，逐字输出)
     *
     * @param aiChatRequest 对话请求
     * @param request       请求对象
     * @return SSE 发射器
     */
    @PostMapping("/streamChat")
    @Operation(summary = "流式实时 AI 对话")
    @OperationLog(module = "AI 对话模块", action = "流式 AI 对话")
    public SseEmitter streamChat(@RequestBody AiChatRequest aiChatRequest, HttpServletRequest request) {
        // 使用配置的超时时间
        SseEmitter emitter = new SseEmitter(knowledgeProperties.getSseTimeout());
        aiChatService.streamChat(aiChatRequest, emitter, request);
        return emitter;
    }

    /**
     * 获取系统支持的 AI 模型列表
     *
     * @return 模型信息列表
     */
    @GetMapping("/models")
    @Operation(summary = "获取支持的 AI 模型列表")
    public BaseResponse<List<AiModelVO>> listModels() {
        List<AiModelVO> models = List.of(
                AiModelVO.builder().name("dashscope").description("通义千问 (阿里巴巴)").build(),
                AiModelVO.builder().name("ollama").description("本地 Ollama 模型").build()
        );
        return ResultUtils.success(models);
    }
}
