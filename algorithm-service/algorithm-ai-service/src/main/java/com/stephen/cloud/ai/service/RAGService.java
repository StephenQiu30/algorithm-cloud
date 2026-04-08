package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.ai.model.dto.rag.BatchRecallRequest;
import com.stephen.cloud.api.ai.model.dto.rag.RAGHistoryQueryRequest;
import com.stephen.cloud.api.ai.model.dto.rag.RecallAnalysisRequest;
import com.stephen.cloud.api.ai.model.vo.BatchRecallVO;
import com.stephen.cloud.api.ai.model.vo.RAGHistoryVO;
import com.stephen.cloud.api.ai.model.vo.RAGStreamEventVO;
import com.stephen.cloud.api.ai.model.vo.RecallAnalysisVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * RAG检索增强生成服务接口
 * <p>
 * 提供基于知识库的问答能力，核心流程包括：问题改写、向量检索、Rerank重排序、生成回答。
 * 支持流式输出（Server-Sent Events）以提升用户体验。
 * </p>
 *
 * @author StephenQiu30
 */
public interface RAGService {

    /**
     * 流式问答
     * <p>
     * 根据问题在指定知识库中进行检索增强生成，并通过响应体流式输出答案。
     *
     * @param question        用户问题
     * @param knowledgeBaseId 知识库 ID
     * @param userId          用户 ID
     * @param topK            召回片段数量
     * @return 答案片段的流
     */
    Flux<String> askStream(String question, Long knowledgeBaseId, Long userId, Integer topK,
                           String conversationId, Boolean enableWebSearchFallback);

    /**
     * 结构化 SSE 流式问答
     * <p>
     * 相比纯文本流，额外返回阶段事件，便于前端更早展示“正在检索/正在生成”等状态。
     * </p>
     */
    Flux<ServerSentEvent<RAGStreamEventVO>> askEventStream(String question, Long knowledgeBaseId, Long userId,
                                                           Integer topK, String conversationId,
                                                           Boolean enableWebSearchFallback);

    /**
     * 保存问答历史
     *
     * @param question      用户问题
     * @param answer       生成答案
     * @param knowledgeBaseId 知识库 ID
     * @param userId       用户 ID
     * @param sources      召回来源片段（JSON 字符串）
     * @param responseTime 响应耗时（毫秒）
     */
    void saveHistory(String question, String answer, Long knowledgeBaseId, Long userId, String sources, Long responseTime);

    /**
     * 分页获取问答历史
     *
     * @param queryRequest 查询请求
     * @param request      HTTP 请求
     * @return 问答历史分页数据
     */
    Page<RAGHistoryVO> listRAGHistoryVOByPage(RAGHistoryQueryRequest queryRequest, HttpServletRequest request);

    /**
     * 召回效果分析
     * <p>
     * 对单个问题进行召回分析，返回各片段与问题的相关性评分。
     *
     * @param request 分析请求
     * @return 包含各片段相关性评分的分析结果
     */
    RecallAnalysisVO analyzeRecall(RecallAnalysisRequest request);

    /**
     * 批量召回效果分析
     * <p>
     * 批量对多个问题进行召回分析，用于评估检索系统整体性能。
     *
     * @param request 批量分析请求
     * @return 批量分析结果
     */
    BatchRecallVO batchAnalyzeRecall(BatchRecallRequest request);
}
