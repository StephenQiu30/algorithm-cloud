package com.stephen.cloud.ai.service.impl;

import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.knowledge.context.RagSearchContext;
import com.stephen.cloud.ai.manager.KnowledgeChunkSearchFacade;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.ai.service.KnowledgeService;
import com.stephen.cloud.ai.service.RagService;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordDTO;
import com.stephen.cloud.api.ai.model.enums.AiModelTypeEnum;
import com.stephen.cloud.api.ai.model.enums.AiToolEnum;
import com.stephen.cloud.api.knowledge.model.dto.RagChatRequest;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import com.stephen.cloud.api.knowledge.model.vo.RagChatResponseVO;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 服务实现类：提供面向排序算法教学的检索增强问答。
 * <p>
 * 该实现基于 Spring AI 的 Tool Calling 模型实现 Agentic RAG。
 * 核心流程：
 * 1. 接收用户问题。
 * 2. 调度 LLM (DashScope) 决策是否需要调用 `algorithmKnowledgeSearch` 工具。
 * 3. 工具执行混合检索（kNN + BM25）并注入上下文。
 * 4. LLM 基于检索到的分片生成包含引用标记的回答。
 * </p>
 *
 * @author StephenQiu30
 */
@Service
public class RagServiceImpl implements RagService {

    /**
     * 系统提示词：定义教育专家角色、RAG 规范及输出要求。
     */
    private static final String SYSTEM_PROMPT = """
            # 角色
            你是一个专业且耐心的【排序算法教学助教】。
            你的任务是基于提供的知识库内容，为学习者解答关于排序算法（原理、实现、复杂度等）的问题。

            # 核心能力
            1. **精准检索**：当问题涉及算法具体逻辑时，必须使用 `algorithmKnowledgeSearch` 工具。
            2. **对比分析**：擅长使用 Markdown 表格对比各种排序算法的时间/空间复杂度和稳定性。
            3. **正确引用**：在回答的关键结论后，必须标注引用的文档名称。

            # 交互规范
            - **优先参考资料**：检索到的知识库分片是你最可靠的依据。
            - **引用标注**：引自文档的内容请在句末标注 `[1] 文档名称`。
            - **代码规范**：代码实现需清晰、简洁，并包含必要的注释。
            - **透明沟通**：如果检索不到相关算法，请诚实说明并在通用知识基础上尝试回答，提示“该信息未直接出现在知识库中”。

            # 当前知识库上下文
            知识库描述：%s
            """;

    @Resource
    private ChatClient chatClient;
    
    @Resource
    private ChatMemory chatMemory;

    /**
     * 知识分片检索门面：支持混合检索、RRF 重排序及阈值过滤。
     */
    @Resource
    private KnowledgeChunkSearchFacade knowledgeChunkSearchFacade;

    /**
     * 对话录制服务：异步持久化 Token 消耗及消息内容。
     */
    @Resource
    private AiChatRecordService aiChatRecordService;

    /**
     * 知识库元数据服务：获取库描述等信息。
     */
    @Resource
    private KnowledgeService knowledgeService;

    /**
     * 执行 RAG 同步对话。
     *
     * @param request 对话请求对象（含问题、ID、会话 ID）
     * @param userId  当前操作用户 ID
     * @return 包含回复及检索源引用列表的响应对象
     */
    @Override
    public RagChatResponseVO ragChat(RagChatRequest request, Long userId) {
        ThrowUtils.throwIf(request == null || StringUtils.isBlank(request.getQuestion()), ErrorCode.PARAMS_ERROR, "提问内容不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(request.getSessionId()), ErrorCode.PARAMS_ERROR, "会话 ID 不能为空");
        Long knowledgeBaseId = request.getKnowledgeBaseId();
        ThrowUtils.throwIf(knowledgeBaseId == null || knowledgeBaseId <= 0, ErrorCode.PARAMS_ERROR, "知识库 ID 不能为空");

        KnowledgeBase kb = knowledgeService.getById(knowledgeBaseId);
        ThrowUtils.throwIf(kb == null, ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        String kbDesc = StringUtils.defaultIfBlank(kb.getDescription(), "通用排序算法知识库");

        // 1. 初始化检索上下文，确保存储最新的检索分片
        RagSearchContext.clear();

        try {
            // 2. 构造用户提示词
            String userPrompt = String.format("""
                    问题：%s
                    知识库 ID: %d
                    请优先检索并据此回答。
                    """, request.getQuestion().trim(), knowledgeBaseId);

            // 3. 执行 AI 调用（含工具调度）
            ChatResponse resp = chatClient.prompt()
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.getSessionId())
                            .advisors(
                                    MessageChatMemoryAdvisor.builder(chatMemory).build(),
                                    ToolCallAdvisor.builder().build()))
                    .system(String.format(SYSTEM_PROMPT, kbDesc))
                    .user(userPrompt)
                    .toolNames(AiToolEnum.ALGORITHM_KNOWLEDGE_SEARCH.getValue())
                    .call()
                    .chatResponse();

            String answer = resp.getResult().getOutput().getText();
            Usage usage = resp.getMetadata().getUsage();

            // 4. 从上下文中获取本次请求捕获的所有检索源
            List<ChunkSourceVO> sources = RagSearchContext.getAndClearSources();

            // 5. 记录对话记录
            aiChatRecordService.saveAiChatRecordAsync(AiChatRecordDTO.builder()
                    .userId(userId)
                    .sessionId(request.getSessionId())
                    .message(request.getQuestion().trim())
                    .response(answer)
                    .modelType(AiModelTypeEnum.AGENTIC_RAG.getValue())
                    .totalTokens(usage != null ? usage.getTotalTokens().intValue() : 0)
                    .promptTokens(usage != null ? usage.getPromptTokens().intValue() : 0)
                    .completionTokens(usage != null ? usage.getCompletionTokens().intValue() : 0)
                    .build());

            return RagChatResponseVO.builder()
                    .answer(answer)
                    .sources(sources) 
                    .build();
        } finally {
            RagSearchContext.clear();
        }
    }
}
