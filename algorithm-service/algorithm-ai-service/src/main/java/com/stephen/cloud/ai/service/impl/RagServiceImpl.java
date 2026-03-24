package com.stephen.cloud.ai.service.impl;

import com.stephen.cloud.api.knowledge.model.enums.RagMetricTagEnum;
import com.stephen.cloud.ai.knowledge.context.RagSearchContext;
import com.stephen.cloud.ai.knowledge.retrieval.KnowledgeDocumentRetriever;
import com.stephen.cloud.ai.manager.RagAuditManager;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.ai.service.KnowledgeBaseService;
import com.stephen.cloud.ai.service.RagService;
import com.stephen.cloud.ai.enums.RagMetricEnum;
import com.stephen.cloud.api.knowledge.model.dto.rag.RagChatRequest;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import com.stephen.cloud.api.knowledge.model.vo.RagChatResponseVO;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RAG 服务实现类：提供面向排序算法教学的检索增强问答。
 * <p>
 * 该实现基于 Spring AI 的 RetrievalAugmentationAdvisor 实现。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Service
public class RagServiceImpl implements RagService {

    @Resource
    private ChatClient chatClient;

    @Resource
    private ChatMemory chatMemory;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private KnowledgeDocumentRetriever knowledgeDocumentRetriever;

    @Resource
    private RagAuditManager ragAuditManager;

    @Resource
    private MeterRegistry meterRegistry;

    /**
     * 系统提示词模版
     */
    private static final String SYSTEM_PROMPT = """
            # 角色
            你是一个专业的算法教学助手，专注于排序算法及相关计算机科学基础。你的目标是提供准确、深入且易于理解的算法讲解。
            
            # 知识库背景
            你当前关联的知识库描述：[%s]
            
            # 回答规范
            1. **基于上下文**：优先根据检索到的知识库内容回答。如果当前上下文不足以完整回答，请明确提示用户，并基于通用算法知识提供补充，但需注明“补充知识”。
            2. **代码质量**：涉及算法实现时，请提供符合 Google Java 编程规范的代码。
            3. **多维分析**：讲解算法时，务必包含：
               - 核心思想（通俗比喻）
               - 时间复杂度（最好、最快、平均）
               - 空间复杂度
               - 稳定性分析
               - 适用场景与优缺点
            4. **语气与风格**：专业、严谨、耐心地解答，鼓励学生思考底层原理。
            5. **引用来源**：如果引用了特定的文档片段，请在合适的位置提及。
            """;

    @Override
    public RagChatResponseVO ragChat(RagChatRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        // 1. 参数校验
        if (request == null || StringUtils.isBlank(request.getQuestion())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数非法");
        }
        Long kbId = request.getKnowledgeBaseId();
        String sessionId = request.getSessionId();
        log.info("[RAG问答] 收到统一问答请求: userId={}, kbId={}, sessionId={}, question='{}'", 
                userId, kbId, sessionId, request.getQuestion().trim());
        
        ThrowUtils.throwIf(kbId == null || kbId <= 0, ErrorCode.PARAMS_ERROR, "知识库 ID 非法");
        ThrowUtils.throwIf(StringUtils.isBlank(sessionId), ErrorCode.PARAMS_ERROR, "会话 ID 不能为空");

        // 2. 获取知识库详情
        KnowledgeBase kb = knowledgeBaseService.getById(kbId);
        if (kb == null) {
            log.error("[RAG问答] 知识库不存在: kbId={}", kbId);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        }
        String kbDesc = StringUtils.defaultIfBlank(kb.getDescription(), "通用算法知识库");

        // 3. 准备 RAG Advisor
        RetrievalAugmentationAdvisor retrievalAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(knowledgeDocumentRetriever)
                .build();

        try {
            // 4. 执行问答
            ChatResponse response = chatClient.prompt()
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId)
                            .param(KnowledgeDocumentRetriever.KNOWLEDGE_BASE_ID_CONTEXT_KEY, kbId)
                            .param(KnowledgeDocumentRetriever.TOP_K_CONTEXT_KEY, request.getTopK())
                            .advisors(
                                    MessageChatMemoryAdvisor.builder(chatMemory).build(),
                                    retrievalAdvisor))
                    .system(String.format(SYSTEM_PROMPT, kbDesc))
                    .user(request.getQuestion().trim())
                    .call()
                    .chatResponse();

            // 5. 组装结果
            String answer = response.getResult().getOutput().getText();
            List<ChunkSourceVO> sources = (List<ChunkSourceVO>) RagSearchContext.getAndClearSources();

            // 6. 异步审计对话记录
            ragAuditManager.auditCall(userId, sessionId, request.getQuestion(), response, sources);

            long duration = System.currentTimeMillis() - startTime;
            Timer.builder(RagMetricEnum.RAG_LATENCY_MS.getValue())
                    .tag(RagMetricTagEnum.MODE_TAG_KEY.getValue(), RagMetricTagEnum.CALL_MODE_SYNC.getValue())
                    .register(meterRegistry)
                    .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
            log.info("[RAG问答] 问答执行成功: userId={}, sessionId={}, 耗时={}ms, 关联来源数={}", 
                    userId, sessionId, duration, sources.size());

            return RagChatResponseVO.builder()
                    .answer(answer)
                    .sources(sources)
                    .build();
        } finally {
            RagSearchContext.clear();
        }
    }

    @Override
    public Flux<RagChatResponseVO> streamRagChat(RagChatRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        // 1. 参数校验
        if (request == null || StringUtils.isBlank(request.getQuestion())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数非法");
        }
        Long kbId = request.getKnowledgeBaseId();
        String sessionId = request.getSessionId();
        log.info("[RAG流式] 收到流式问答请求: userId={}, kbId={}, sessionId={}", userId, kbId, sessionId);
        
        ThrowUtils.throwIf(kbId == null || kbId <= 0, ErrorCode.PARAMS_ERROR, "知识库 ID 非法");
        ThrowUtils.throwIf(StringUtils.isBlank(sessionId), ErrorCode.PARAMS_ERROR, "会话 ID 不能为空");

        // 2. 检查知识库
        KnowledgeBase kb = knowledgeBaseService.getById(kbId);
        if (kb == null) {
            log.error("[RAG流式] 知识库不存在: kbId={}", kbId);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        }
        String kbDesc = StringUtils.defaultIfBlank(kb.getDescription(), "通用算法知识库");

        RagChatResponseVO firstChunk = RagChatResponseVO.builder().sources(List.of()).build();

        // 4. 准备流式输出
        StringBuilder fullAnswer = new StringBuilder();
        RetrievalAugmentationAdvisor retrievalAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(knowledgeDocumentRetriever)
                .build();

        AtomicReference<Usage> usageRef = new AtomicReference<>();
        Flux<RagChatResponseVO> contentFlux = chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId)
                        .param(KnowledgeDocumentRetriever.KNOWLEDGE_BASE_ID_CONTEXT_KEY, kbId)
                        .param(KnowledgeDocumentRetriever.TOP_K_CONTEXT_KEY, request.getTopK())
                        .advisors(
                                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                                retrievalAdvisor))
                .system(String.format(SYSTEM_PROMPT, kbDesc))
                .user(request.getQuestion().trim())
                .stream()
                .chatResponse()
                .map(chatResponse -> {
                    Usage usage = chatResponse.getMetadata() != null ? chatResponse.getMetadata().getUsage() : null;
                    if (usage != null) {
                        usageRef.set(usage);
                    }
                    String text = chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null
                            ? chatResponse.getResult().getOutput().getText() : "";
                    fullAnswer.append(text);
                    return RagChatResponseVO.builder().answer(text).build();
                });

        // 5. 组装并处理收尾逻辑
        return Flux.concat(
                Flux.just(firstChunk),
                contentFlux
        ).doOnComplete(() -> {
            long duration = System.currentTimeMillis() - startTime;
            Timer.builder(RagMetricEnum.RAG_LATENCY_MS.getValue())
                    .tag(RagMetricTagEnum.MODE_TAG_KEY.getValue(), RagMetricTagEnum.CALL_MODE_STREAM.getValue())
                    .register(meterRegistry)
                    .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);
            log.info("[RAG流式] 问答流输出完成: userId={}, sessionId={}, 总长度={}", 
                    userId, sessionId, fullAnswer.length());
            // 异步记录审计
            List<ChunkSourceVO> sources = (List<ChunkSourceVO>) RagSearchContext.getAndClearSources();
            ragAuditManager.auditStream(userId, sessionId, request.getQuestion(), fullAnswer.toString(), usageRef.get(), sources);
        }).doOnError(e -> {
            log.error("[RAG流式] 问答流执行异常: userId={}, sessionId={}, error={}", userId, sessionId, e.getMessage(), e);
            RagSearchContext.clear();
        });
    }
}
