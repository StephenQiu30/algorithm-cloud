package com.stephen.cloud.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stephen.cloud.ai.model.entity.AiChatRecord;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.ai.service.AiChatService;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordDTO;
import com.stephen.cloud.api.ai.model.dto.AiChatRequest;
import com.stephen.cloud.api.ai.model.vo.AiChatResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * AI 对话服务实现类
 * <p>
 * 对接 Spring AI 框架，提供多模型适配、历史会话加载、标准/流式输出及异步异步持久化能力。
 * </p>
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    @Resource
    private ChatClient chatClient;

    @Resource
    private AiChatRecordService aiChatRecordService;

    /**
     * AI 标准同步对话
     *
     * @param aiChatRequest 对话请求参数
     * @param request       HTTP 请求
     * @return AI 完整响应结果
     */
    @Override
    public AiChatResponse chat(AiChatRequest aiChatRequest, HttpServletRequest request) {
        log.info("执行 AI 标准对话: sessionId={}, message={}", aiChatRequest.getSessionId(), aiChatRequest.getMessage());
        ThrowUtils.throwIf(aiChatRequest == null, ErrorCode.PARAMS_ERROR);
        String message = aiChatRequest.getMessage();
        ThrowUtils.throwIf(StringUtils.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");

        // 构建请求并调用 LLM
        ChatResponse response = chatClient.prompt()
                .user(message)
                .system(aiChatRequest.getSystemMessage())
                .advisors(MessageChatMemoryAdvisor.builder(getChatMemory(aiChatRequest)).build())
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, aiChatRequest.getSessionId()))
                .call()
                .chatResponse();

        String responseText = response.getResult().getOutput().getText();
        Usage usage = response.getMetadata().getUsage();

        // 异步持久化对话记录
        aiChatRecordService.saveAiChatRecordAsync(AiChatRecordDTO.builder()
                .sessionId(aiChatRequest.getSessionId())
                .postId(aiChatRequest.getPostId())
                .message(message)
                .response(responseText)
                .modelType(aiChatRequest.getModelType())
                .totalTokens(usage != null ? usage.getTotalTokens().intValue() : 0)
                .promptTokens(usage != null ? usage.getPromptTokens().intValue() : 0)
                .completionTokens(usage != null ? usage.getCompletionTokens().intValue() : 0)
                .build());

        return AiChatResponse.builder()
                .content(responseText)
                .totalTokens(usage != null ? usage.getTotalTokens().intValue() : 0)
                .promptTokens(usage != null ? usage.getPromptTokens().intValue() : 0)
                .completionTokens(usage != null ? usage.getCompletionTokens().intValue() : 0)
                .build();
    }

    /**
     * AI 流式对话 (SSE)
     *
     * @param aiChatRequest 对话请求参数
     * @param emitter       SSE 发射器
     * @param request       HTTP 请求
     */
    @Override
    public void streamChat(AiChatRequest aiChatRequest, SseEmitter emitter, HttpServletRequest request) {
        log.info("执行 AI 流式对话: sessionId={}", aiChatRequest.getSessionId());
        ThrowUtils.throwIf(aiChatRequest == null, ErrorCode.PARAMS_ERROR);
        String message = aiChatRequest.getMessage();
        ThrowUtils.throwIf(StringUtils.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");

        StringBuilder fullResponse = new StringBuilder();
        
        // 响应式生成 Token 并推送到前端
        chatClient.prompt()
                .user(message)
                .system(aiChatRequest.getSystemMessage())
                .advisors(MessageChatMemoryAdvisor.builder(getChatMemory(aiChatRequest)).build())
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, aiChatRequest.getSessionId()))
                .stream()
                .content()
                .doOnNext(token -> {
                    try {
                        fullResponse.append(token);
                        emitter.send(SseEmitter.event().data(token));
                    } catch (IOException e) {
                        log.error("SSE 推送异常", e);
                    }
                })
                .doOnComplete(() -> {
                    log.info("AI 流式生成完成");
                    // 结束后异步保存完整记录
                    aiChatRecordService.saveAiChatRecordAsync(AiChatRecordDTO.builder()
                            .sessionId(aiChatRequest.getSessionId())
                            .postId(aiChatRequest.getPostId())
                            .message(message)
                            .response(fullResponse.toString())
                            .modelType(aiChatRequest.getModelType())
                            .build());
                    emitter.complete();
                })
                .doOnError(error -> {
                    log.error("AI 流式生成异常", error);
                    emitter.completeWithError(error);
                })
                .subscribe();
    }

    /**
     * 获取并加载历史会话记录填充 ChatMemory
     * <p>
     * 自动从数据库拉取最近的对话往返，维持对话连贯性。
     * </p>
     *
     * @param aiChatRequest 请求参数
     * @return ChatMemory
     */
    private ChatMemory getChatMemory(AiChatRequest aiChatRequest) {
        String sessionId = aiChatRequest.getSessionId();
        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(100)
                .build();
        if (StringUtils.isNotBlank(sessionId)) {
            // 加载最近 20 条对话记录作为上下文
            List<AiChatRecord> history = aiChatRecordService.list(
                    new LambdaQueryWrapper<AiChatRecord>()
                            .select(AiChatRecord::getMessage, AiChatRecord::getResponse)
                            .eq(AiChatRecord::getSessionId, sessionId)
                            .orderByDesc(AiChatRecord::getCreateTime)
                            .last("limit 20"));
            Collections.reverse(history);
            for (AiChatRecord record : history) {
                memory.add(sessionId, new UserMessage(record.getMessage()));
                memory.add(sessionId, new AssistantMessage(record.getResponse()));
            }
        }
        return memory;
    }
}
