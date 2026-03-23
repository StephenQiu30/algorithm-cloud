package com.stephen.cloud.ai.manager;

import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordDTO;
import com.stephen.cloud.api.ai.model.enums.AiModelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * RAG 审计策略管理器：统一处理问答记录的异步持久化。
 * <p>
 * 采用解耦设计，确保 RAG 核心逻辑与审计功能分离，提升可维护性。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class RagAuditManager {

    @Resource
    private AiChatRecordService aiChatRecordService;

    /**
     * 同步问答审计
     */
    public void auditCall(Long userId, String sessionId, String question, ChatResponse response) {
        if (response == null || response.getResult() == null) {
            return;
        }
        String answer = response.getResult().getOutput().getText();
        Usage usage = response.getMetadata().getUsage();
        
        saveRecord(userId, sessionId, question, answer, usage);
    }

    /**
     * 流式问答审计 (完成后调用)
     */
    public void auditStream(Long userId, String sessionId, String question, String fullAnswer) {
        saveRecord(userId, sessionId, question, fullAnswer, null);
    }

    private void saveRecord(Long userId, String sessionId, String question, String answer, Usage usage) {
        try {
            AiChatRecordDTO record = AiChatRecordDTO.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .message(question != null ? question.trim() : "")
                    .response(answer)
                    .modelType(AiModelTypeEnum.AGENTIC_RAG.getValue())
                    .totalTokens(usage != null && usage.getTotalTokens() != null ? usage.getTotalTokens().intValue() : 0)
                    .promptTokens(usage != null && usage.getPromptTokens() != null ? usage.getPromptTokens().intValue() : 0)
                    .completionTokens(0) // 当前版本 Usage 未提供 getGenerationTokens 方法
                    .build();
            
            aiChatRecordService.saveAiChatRecordAsync(record);
        } catch (Exception e) {
            log.error("保存 RAG 审计记录异常: {}", e.getMessage(), e);
        }
    }
}
