package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.convert.Convert;
import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.ai.service.KnowledgeService;
import com.stephen.cloud.ai.service.RagService;
import com.stephen.cloud.ai.service.VectorStoreService;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordDTO;
import com.stephen.cloud.api.knowledge.model.dto.RagChatRequest;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import com.stephen.cloud.api.knowledge.model.vo.RagChatResponseVO;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 服务实现
 * <p>
 * 整合向量数据库检索 (Retrieval) 与大语言模型生成 (Generation)，实现基于私有知识库的语义增强问答。
 * 该服务支持动态 TopK 调节、相似度阈值过滤以及多维度的来源溯源信息展示。
 * </p>
 *
 * @author StephenQiu30
 */
@Service
public class RagServiceImpl implements RagService {

    /**
     * 系统提示词模版 - 面向算法教学的教育专家
     */
    private static final String SYS = """
            你是一位资深的数据结构与算法教育专家。你的任务是根据「参考资料」为学生讲解排序算法。

            --------
            参考资料：
            %s
            --------

            要求：
            1. **专业且通俗**：使用专业的术语，但能用生动的比喻或步骤说明让初学者理解。
            2. **结构化回答**：
               - **基本原理**：简述算法的核心思想。
               - **执行流程**：给出该算法的关键步骤。
               - **复杂度分析**：明确指出最好、最坏、平均时间复杂度和空间复杂度。
               - **代码示例**：如果可能，提供简洁的伪代码或 Java 实现。
            3. **交互鼓励**：在回答结束时，可以引导学生思考，例如：“你想看看这个算法在特定数组上的执行过程吗？”
            4. **资料约束**：仅依赖提供的「参考资料」作答。如果资料不足以回答，请礼貌地说明，并根据你的通用算法知识给出基础性建议，但需注明“补充知识”。
            """;

    @Resource
    private ChatClient chatClient;

    @Resource
    private VectorStoreService vectorStoreService;

    @Resource
    private AiChatRecordService aiChatRecordService;

    @Resource
    private KnowledgeService knowledgeService;

    @Resource
    private KnowledgeProperties knowledgeProperties;

    /**
     * 执行 RAG 对话
     *
     * @param request         问答请求参数
     * @param userId          当前操作用户 ID
     * @return RAG 问答结果封装
     */
    @Override
    public RagChatResponseVO ragChat(RagChatRequest request, Long userId) {
        Long knowledgeBaseId = request.getKnowledgeBaseId();
        if (request == null || StringUtils.isBlank(request.getQuestion())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提问内容不能为空");
        }
        // 1. 权限与所有权检查
        knowledgeService.getAndCheckAccess(knowledgeBaseId, userId);

        // 2. 准备向量搜索请求
        int topK = request.getTopK() != null && request.getTopK() > 0
                ? request.getTopK()
                : knowledgeProperties.getDefaultTopK();
        if (topK > 20) {
            topK = 20; // 约束检索上限
        }

        // 构造 Metadata 过滤条件，确保只从该知识库检索
        String filter = "knowledgeBaseId == '" + knowledgeBaseId + "'";
        SearchRequest sr = SearchRequest.builder()
                .query(request.getQuestion().trim())
                .topK(topK)
                .similarityThreshold(0.0) // 基础阈值，由向量库具体权重控制
                .filterExpression(filter)
                .build();

        // 3. 执行相似度搜索
        List<Document> hits = vectorStoreService.similaritySearch(sr);
        if (hits.isEmpty()) {
            return RagChatResponseVO.builder()
                    .answer("根据现有资料无法回答。")
                    .sources(List.of())
                    .build();
        }

        // 4. 构建上下文 Prompt
        StringBuilder ctx = new StringBuilder();
        int n = 1;
        List<ChunkSourceVO> sources = new ArrayList<>();
        for (Document d : hits) {
            String text = d.getText();
            ctx.append("[").append(n).append("] ").append(text).append("\n");
            // 封装来源信息
            Long chunkId = Convert.toLong(d.getMetadata().get("chunkId"));
            double score = d.getScore() != null ? d.getScore() : 0.0;
            sources.add(ChunkSourceVO.builder().chunkId(chunkId).content(text).score(score).build());
            n++;
        }

        // 5. 调用 LLM 生成回答
        String system = String.format(SYS, ctx.toString());
        ChatResponse resp = chatClient.prompt()
                .system(system)
                .user(u -> u.text("用户问题：" + request.getQuestion().trim()))
                .call()
                .chatResponse();

        String answer = resp.getResult().getOutput().getText();
        Usage usage = resp.getMetadata().getUsage();

        // 异步持久化对话记录
        aiChatRecordService.saveAiChatRecordAsync(AiChatRecordDTO.builder()
                .userId(userId)
                .sessionId(request.getSessionId())
                .message(request.getQuestion().trim())
                .response(answer)
                .modelType("RAG")
                .totalTokens(usage != null ? usage.getTotalTokens().intValue() : 0)
                .promptTokens(usage != null ? usage.getPromptTokens().intValue() : 0)
                .completionTokens(usage != null ? usage.getCompletionTokens().intValue() : 0)
                .build());

        return RagChatResponseVO.builder()
                .answer(answer)
                .sources(sources)
                .build();
    }
}
