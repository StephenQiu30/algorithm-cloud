package com.stephen.cloud.ai.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stephen.cloud.ai.model.entity.AiChatRecord;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.ai.service.AiChatService;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordDTO;
import com.stephen.cloud.api.ai.model.dto.AiChatRequest;
import com.stephen.cloud.api.ai.model.vo.AiChatResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
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

    @Resource
    private RabbitMqSender mqSender;

    /**
     * AI 标准对话
     */
    @Override
    public AiChatResponse chat(AiChatRequest aiChatRequest, HttpServletRequest request) {
        log.info("执行 AI 标准对话: message={}", aiChatRequest.getMessage());
        ThrowUtils.throwIf(aiChatRequest == null, ErrorCode.PARAMS_ERROR);
        String message = aiChatRequest.getMessage();
        ThrowUtils.throwIf(StringUtils.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");

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
        saveChatRecordAsync(aiChatRequest, message, responseText, usage);

        AiChatResponse vo = AiChatResponse.builder()
                .content(response.getResult().getOutput().getText())
                .totalTokens(usage != null ? usage.getTotalTokens().intValue() : 0)
                .promptTokens(usage != null ? usage.getPromptTokens().intValue() : 0)
                .completionTokens(usage != null ? usage.getCompletionTokens().intValue() : 0)
                .build();
        return vo;
    }

    /**
     * AI 流式对话 (SSE)
     */
    @Override
    public void streamChat(AiChatRequest aiChatRequest, SseEmitter emitter, HttpServletRequest request) {
        log.info("执行 AI 流式对话: message={}", aiChatRequest.getMessage());
        ThrowUtils.throwIf(aiChatRequest == null, ErrorCode.PARAMS_ERROR);
        String message = aiChatRequest.getMessage();
        ThrowUtils.throwIf(StringUtils.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");

        StringBuilder fullResponse = new StringBuilder();
        
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
                    // 异步保存记录
                    saveChatRecordAsync(aiChatRequest, message, fullResponse.toString(), null);
                    emitter.complete();
                })
                .doOnError(error -> {
                    log.error("AI 流式生成异常", error);
                    emitter.completeWithError(error);
                })
                .subscribe();
    }

    private ChatMemory getChatMemory(AiChatRequest aiChatRequest) {
        String sessionId = aiChatRequest.getSessionId();
        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(100)
                .build();
        if (StringUtils.isNotBlank(sessionId)) {
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

    private void saveChatRecordAsync(AiChatRequest aiChatRequest, String message, String response, Usage usage) {
        try {
            Long userId = SecurityUtils.getLoginUserIdPermitNull();
            AiChatRecordDTO aiChatRecordDTO = AiChatRecordDTO.builder()
                    .userId(userId)
                    .sessionId(aiChatRequest.getSessionId())
                    .postId(aiChatRequest.getPostId())
                    .message(message)
                    .response(response)
                    .modelType(aiChatRequest.getModelType())
                    .totalTokens(usage != null ? usage.getTotalTokens().intValue() : 0)
                    .promptTokens(usage != null ? usage.getPromptTokens().intValue() : 0)
                    .completionTokens(usage != null ? usage.getCompletionTokens().intValue() : 0)
                    .build();
            String bizId = "ai_chat:" + System.currentTimeMillis();
            mqSender.send(MqBizTypeEnum.AI_CHAT_RECORD, bizId, aiChatRecordDTO);
        } catch (Exception e) {
            log.error("异步同步 AI 对话记录失败", e);
        }
    }
}
