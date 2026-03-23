package com.stephen.cloud.ai.service;

import com.stephen.cloud.api.knowledge.model.dto.retrieval.KnowledgeRetrievalRequest;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;

import java.util.List;

/**
 * 知识检索服务
 * <p>
 * 提供针对知识库的语义检索能力，返回相似度评分和源文本切片，用于优化 RAG 效果。
 * </p>
 *
 * @author StephenQiu30
 */
public interface KnowledgeRetrievalService {

    /**
     * 知识库语义检索
     *
     * @param request 检索请求
     * @param userId  操作人 ID
     * @return 匹配的切片列表
     */
    List<ChunkSourceVO> search(KnowledgeRetrievalRequest request, Long userId);
}
