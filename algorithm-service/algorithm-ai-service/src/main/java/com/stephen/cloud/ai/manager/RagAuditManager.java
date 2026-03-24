package com.stephen.cloud.ai.manager;

import cn.hutool.json.JSONUtil;
import com.stephen.cloud.ai.enums.RagMetricEnum;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordDTO;
import com.stephen.cloud.api.ai.model.enums.AiModelTypeEnum;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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

    @Resource
    private MeterRegistry meterRegistry;

    /**
     * 同步问答审计
     */
    public void auditCall(Long userId, String sessionId, String question, ChatResponse response, List<ChunkSourceVO> sources) {
        if (response == null || response.getResult() == null) {
            return;
        }
        String answer = response.getResult().getOutput().getText();
        Usage usage = response.getMetadata().getUsage();

        saveRecord(userId, sessionId, question, answer, usage, sources);
    }

    /**
     * 流式问答审计 (完成后调用)
     */
    public void auditStream(Long userId, String sessionId, String question, String fullAnswer, Usage usage,
                            List<ChunkSourceVO> sources) {
        saveRecord(userId, sessionId, question, fullAnswer, usage, sources);
    }

    private void saveRecord(Long userId, String sessionId, String question, String answer, Usage usage, List<ChunkSourceVO> sources) {
        try {
            int total = usage != null && usage.getTotalTokens() != null ? usage.getTotalTokens().intValue() : 0;
            int prompt = usage != null && usage.getPromptTokens() != null ? usage.getPromptTokens().intValue() : 0;
            int completion = Math.max(0, total - prompt);
            String retrievalMetadata = JSONUtil.toJsonStr(Map.of(
                    "sources", sources == null ? List.of() : sources,
                    "usageAvailable", usage != null
            ));
            AiChatRecordDTO record = AiChatRecordDTO.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .message(question != null ? question.trim() : "")
                    .response(answer)
                    .modelType(AiModelTypeEnum.AGENTIC_RAG.getValue())
                    .totalTokens(total)
                    .promptTokens(prompt)
                    .completionTokens(completion)
                    .retrievalMetadata(retrievalMetadata)
                    .build();

            aiChatRecordService.saveAiChatRecordAsync(record);
            Counter.builder(RagMetricEnum.RAG_TOKEN_TOTAL.getValue()).register(meterRegistry).increment(total);
        } catch (Exception e) {
            log.error("保存 RAG 审计记录异常: {}", e.getMessage(), e);
        }
    }
}
