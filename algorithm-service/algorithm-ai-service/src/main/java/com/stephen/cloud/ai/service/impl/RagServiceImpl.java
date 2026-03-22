package com.stephen.cloud.ai.service.impl;

import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.service.KnowledgeService;
import com.stephen.cloud.ai.service.RagService;
import com.stephen.cloud.ai.service.VectorStoreService;
import com.stephen.cloud.api.knowledge.model.dto.RagChatRequest;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import com.stephen.cloud.api.knowledge.model.vo.RagChatResponseVO;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 服务实现
 * <p>
 * 整合向量数据库检索 (Retrieval) 与大语言模型生成 (Generation)，实现基于私有知识库的问答。
 * </p>
 *
 * @author StephenQiu30
 */
@Service
public class RagServiceImpl implements RagService {

    /**
     * 系统提示词模版
     */
    private static final String SYS = """
            你是企业知识库问答助手。仅根据「参考资料」作答；资料不足以回答时请明确说明「根据现有资料无法回答」，不要编造。
            --------
            参考资料：
            %s
            --------
            要求：简洁、专业；可引用参考资料编号。""";

    @Resource
    private ChatClient chatClient;

    @Resource
    private VectorStoreService vectorStoreService;

    @Resource
    private KnowledgeService knowledgeService;

    @Resource
    private KnowledgeProperties knowledgeProperties;

    /**
     * 执行 RAG 对话
     *
     * @param knowledgeBaseId 对应知识库 ID
     * @param request         问答请求参数
     * @param userId          当前操作用户 ID
     * @return RAG 问答结果封装
     */
    @Override
    public RagChatResponseVO ragChat(Long knowledgeBaseId, RagChatRequest request, Long userId) {
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
            Long chunkId = parseLong(d.getMetadata().get("chunkId"));
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
        return RagChatResponseVO.builder()
                .answer(answer)
                .sources(sources)
                .build();
    }

    /**
     * 解析 Metadata 中的长整型
     *
     * @param o 对象
     * @return Long
     */
    private static Long parseLong(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
