package com.stephen.cloud.ai.service.impl;

import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.manager.KnowledgeChunkSearchFacade;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.ai.service.KnowledgeService;
import com.stephen.cloud.ai.service.RagService;
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
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 服务实现：本项目智能体定位为<strong>面向排序算法教学</strong>的检索增强问答。
 * <p>
 * 检索经 {@link com.stephen.cloud.ai.service.VectorStoreService}（可含混合检索）→ 拼参考资料与系统提示 → {@link ChatClient} 生成 → 异步写入聊天记录。
 * {@link com.stephen.cloud.api.knowledge.model.dto.RagChatRequest#getSessionId()} 仅用于记录关联，不注入历史对话。
 * </p>
 *
 * @author StephenQiu30
 */
@Service
public class RagServiceImpl implements RagService {

    private static final String SYS = """
            你是本项目面向排序算法教学的智能体（助教角色）。当前知识库说明：%s

            --------
            参考资料：
            %s
            --------

            要求：
            1. 仅依据「参考资料」作答；不足则说明无法从库中得出，可补充通用常识并标注「补充说明」。
            2. 面向学习者：先交代思路再展开，必要时对比不同排序的时间/空间复杂度，与参考资料中的表述一致。
            3. 涉及步骤、循环不变式、复杂度时与参考资料保持一致，不臆造实现细节。
            """;

    @Resource
    private ChatClient chatClient;

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
        if (request == null || StringUtils.isBlank(request.getQuestion())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提问内容不能为空");
        }
        Long knowledgeBaseId = request.getKnowledgeBaseId();
        if (knowledgeBaseId == null || knowledgeBaseId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库 ID 不能为空");
        }

        KnowledgeBase kb = knowledgeService.getById(knowledgeBaseId);
        if (kb == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        }
        String kbDesc = StringUtils.defaultIfBlank(kb.getDescription(), "未填写说明，请严格依据参考资料作答。");

        List<ChunkSourceVO> sources = knowledgeChunkSearchFacade.searchChunksForVerifiedKnowledgeBase(
                knowledgeBaseId,
                request.getQuestion().trim(),
                request.getTopK(),
                knowledgeProperties.getRagTopKMax());
        if (sources.isEmpty()) {
            return RagChatResponseVO.builder()
                    .answer("根据现有资料无法回答。")
                    .sources(List.of())
                    .build();
        }

        StringBuilder ctx = new StringBuilder();
        int n = 1;
        for (ChunkSourceVO s : sources) {
            String text = s.getContent();
            ctx.append("[").append(n).append("] ").append(text).append("\n");
            n++;
        }

        String system = String.format(SYS, kbDesc, ctx.toString());
        ChatResponse resp = chatClient.prompt()
                .system(system)
                .user(u -> u.text("用户问题：" + request.getQuestion().trim()))
                .call()
                .chatResponse();

        String answer = resp.getResult().getOutput().getText();
        Usage usage = resp.getMetadata().getUsage();

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
