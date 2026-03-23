package com.stephen.cloud.ai.service;

import com.stephen.cloud.api.knowledge.model.dto.RagChatRequest;
import com.stephen.cloud.api.knowledge.model.vo.RagChatResponseVO;

/**
 * RAG 对话服务（检索增强生成）。本项目智能体面向<strong>排序算法教学</strong>，具体知识点以知识库文档与
 * {@link com.stephen.cloud.ai.model.entity.KnowledgeBase#getDescription() 知识库说明} 为准。
 *
 * @author StephenQiu30
 */
public interface RagService {

    /**
     * 基于指定知识库发起问答：检索（经 {@link VectorStoreService}，可含混合检索）后拼 prompt 并调用大模型。
     * <p>
     * {@link RagChatRequest#getSessionId()} 必填，用作 {@link org.springframework.ai.chat.memory.ChatMemory} 会话隔离与落库关联。
     * </p>
     *
     * @param request 问答请求（知识库 ID、问题、可选 topK、会话 ID）
     * @param userId  当前用户 ID，用于鉴权与对话记录
     * @return 模型回答及命中的切片来源列表（含 chunkId、score，供前端溯源）
     */
    RagChatResponseVO ragChat(RagChatRequest request, Long userId);
}
