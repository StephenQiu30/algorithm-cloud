package com.stephen.cloud.ai.service.impl;

import com.stephen.cloud.ai.knowledge.context.RagSearchContext;
import com.stephen.cloud.ai.manager.KnowledgeChunkSearchFacade;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.ai.service.KnowledgeBaseService;
import com.stephen.cloud.ai.service.RagService;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordDTO;
import com.stephen.cloud.api.ai.model.enums.AiModelTypeEnum;
import com.stephen.cloud.api.ai.model.enums.AiToolEnum;
import com.stephen.cloud.api.knowledge.model.dto.rag.RagChatRequest;
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
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 执行 RAG (检索增强生成) 问答流程
     * <p>
     * 核心步骤：
     * 1. 权限与参数预校验。
     * 2. 清理线程上下文中的检索缓存。
     * 3. 构造包含知识库背景的 System Prompt 与用户问题。
     * 4. 驱动 Spring AI ChatClient，通过 Advisor 注入会话记忆与工具调度能力。
     * 5. 若大模型决定调用 `algorithmKnowledgeSearch`，则执行知识库检索并回填上下文。
     * 6. 捕获最终回答、消耗 Token 统计及命中的分片来源，异步持久化对话记录。
     * </p>
     *
     * @param request 包含提问内容、知识库 ID 及会话标识的请求对象
     * @param userId  执行提问的用户 ID
     * @return 包含回复正文及溯源切片列表的视图对象
     * @throws BusinessException 当参数非法、知识库缺失或模型调用严重异常时抛出
     */
    @Override
    public RagChatResponseVO ragChat(RagChatRequest request, Long userId) {
        // 1. 严格参数校验
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(request.getQuestion()), ErrorCode.PARAMS_ERROR, "提问内容不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(request.getSessionId()), ErrorCode.PARAMS_ERROR, "会话 ID 不能为空");
        Long knowledgeBaseId = request.getKnowledgeBaseId();
        ThrowUtils.throwIf(knowledgeBaseId == null || knowledgeBaseId <= 0, ErrorCode.PARAMS_ERROR, "知识库 ID 不能为空");

        // 2. 知识库合法性检查
        KnowledgeBase kb = knowledgeBaseService.getById(knowledgeBaseId);
        ThrowUtils.throwIf(kb == null, ErrorCode.NOT_FOUND_ERROR, "指定的知识库不存在");
        String kbDesc = StringUtils.defaultIfBlank(kb.getDescription(), "通用排序算法知识库");

        // 3. 检索上下文预处理 (确保本次请求的 sources 不受之前请求干扰)
        RagSearchContext.clear();

        try {
            // 4. 构造具备上下文提示的用户 Prompt
            String userPrompt = String.format("""
                    问题：%s
                    知识库 ID: %d
                    请优先根据检索到的知识库分片回答，若分片不足则告知用户。
                    """, request.getQuestion().trim(), knowledgeBaseId);

            // 5. 调用大模型：注入记忆 Advisor 与工具调用 Advisor
            ChatResponse resp = chatClient.prompt()
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.getSessionId())
                            .advisors(
                                    MessageChatMemoryAdvisor.builder(chatMemory).build(),
                                    ToolCallAdvisor.builder().build())) // 支持 Agentic 工具调度
                    .system(String.format(SYSTEM_PROMPT, kbDesc))
                    .user(userPrompt)
                    .toolNames(AiToolEnum.ALGORITHM_KNOWLEDGE_SEARCH.getValue()) // 导出检索工具
                    .call()
                    .chatResponse();

            // 6. 提取回答、统计 Token 消耗
            String answer = resp.getResult().getOutput().getText();
            Usage usage = resp.getMetadata().getUsage();

            // 7. 捕获在工具调用过程中命中的切片来源
            List<ChunkSourceVO> sources = RagSearchContext.getAndClearSources();

            // 8. 异步持久化对话审计记录
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
            // 线程环境清理
            RagSearchContext.clear();
        }
    }
}
