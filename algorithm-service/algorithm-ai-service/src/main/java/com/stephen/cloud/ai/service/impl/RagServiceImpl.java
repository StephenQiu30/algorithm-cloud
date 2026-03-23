package com.stephen.cloud.ai.service.impl;

import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.manager.KnowledgeChunkSearchFacade;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.ai.service.KnowledgeService;
import com.stephen.cloud.ai.service.RagService;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordDTO;
import com.stephen.cloud.api.ai.model.enums.AiModelTypeEnum;
import com.stephen.cloud.api.ai.model.enums.AiToolEnum;
import com.stephen.cloud.api.knowledge.model.dto.RagChatRequest;
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
 * RAG 服务实现：本项目智能体定位为<strong>面向排序算法教学</strong>的检索增强问答。
 * <p>
 * 基于 Spring AI 的 Tool Calling 实现代理式 RAG (Agentic RAG)，由大模型自主决定检索时机。
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
            你是一个专门从事【排序算法教学】的 RAG 增强型交互式系统助教。
            你的目标是为学习者提供精准、专业且具有教育意义的算法指导。

            # 核心能力
            1. **知识搜寻**：当用户询问排序算法相关的代码、原理、复杂度或具体步骤时，你通过 `algorithmKnowledgeSearch` 工具检索私有知识库。
            2. **算法分析**：你能对比不同算法的最优、最坏和平均时间复杂度，以及空间复杂度和稳定性。
            3. **代码演示**：提供清晰的代码实现，并符合参考资料中的逻辑规范。

            # 交互规范
            - **依据库回答**：优先使用工具检索出的内容进行回答。如果检索结果中包含相关算法，请以此为准。
            - **引用标注**：在回答过程中，如果是引自检索到的文档，请务必在对应段落末尾标注引用源，格式为 `[1] 文档名称`。检索结果中的 `documentName` 即为文档名称。
            - **透明度**：如果在知识库中未找到直接内容，请说明“在当前算法库中忽略了直接匹配的信息”，并基于通用知识辅助。
            - **格式化输出**：
                - 使用 Markdown 表格展示复杂度对比。
                - 使用代码块包裹算法实现。
                - 重要结论使用引用区块 (`>`)。
            - **教学引导**：通过解析核心思想（如“分而治之”）来引导学习。

            # 当前知识库上下文
            说明：%s
            """;

    @Resource
    private ChatClient chatClient;
    
    @Resource
    private ChatMemory chatMemory;

    @Resource
    private KnowledgeChunkSearchFacade knowledgeChunkSearchFacade;

    @Resource
    private AiChatRecordService aiChatRecordService;

    @Resource
    private KnowledgeService knowledgeService;

    @Resource
    private KnowledgeProperties knowledgeProperties;

    @Override
    public RagChatResponseVO ragChat(RagChatRequest request, Long userId) {
        ThrowUtils.throwIf(request == null || StringUtils.isBlank(request.getQuestion()), ErrorCode.PARAMS_ERROR, "提问内容不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(request.getSessionId()), ErrorCode.PARAMS_ERROR, "会话 ID 不能为空");
        Long knowledgeBaseId = request.getKnowledgeBaseId();
        ThrowUtils.throwIf(knowledgeBaseId == null || knowledgeBaseId <= 0, ErrorCode.PARAMS_ERROR, "知识库 ID 不能为空");

        KnowledgeBase kb = knowledgeService.getById(knowledgeBaseId);
        ThrowUtils.throwIf(kb == null, ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        String kbDesc = StringUtils.defaultIfBlank(kb.getDescription(), "暂无描述");

        // 构造用户提示词，明确指示使用工具检索该知识库
        String userPrompt = String.format("""
                问题：%s
                请优先通过检索知识库 (ID: %d, 说明: %s) 来回答。
                """, request.getQuestion().trim(), knowledgeBaseId, kbDesc);

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

        // 异步记录对话
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
                .sources(List.of()) 
                .build();
    }
}
