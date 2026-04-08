package com.stephen.cloud.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.service.RAGService;
import com.stephen.cloud.api.ai.model.dto.rag.BatchRecallRequest;
import com.stephen.cloud.api.ai.model.dto.rag.RAGAskRequest;
import com.stephen.cloud.api.ai.model.dto.rag.RAGHistoryQueryRequest;
import com.stephen.cloud.api.ai.model.dto.rag.RecallAnalysisRequest;
import com.stephen.cloud.api.ai.model.vo.BatchRecallVO;
import com.stephen.cloud.api.ai.model.vo.RAGHistoryVO;
import com.stephen.cloud.api.ai.model.vo.RecallAnalysisVO;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * RAG检索增强问答接口
 * <p>
 * 提供基于知识库的检索增强生成问答功能，支持流式输出、召回效果分析等。
 * 系统自动完成问题改写、向量检索、片段召回、Rerank重排序等 RAG 流程。
 * </p>
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/ai/rag")
@Tag(name = "RAGController", description = "RAG检索增强问答")
public class RAGController {

    @Resource
    private RAGService ragService;

    /**
     * RAG流式问答
     * <p>
     * 基于知识库进行问答，支持流式输出答案。
     * 系统会自动检索相关知识片段并生成答案。
     *
     * @param askRequest 问答请求
     * @return 流式答案
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "RAG流式问答", description = "基于知识库进行问答，支持流式输出答案")
    @OperationLog(module = "RAG问答", action = "RAG流式问答")
    public Flux<String> askStream(@RequestBody RAGAskRequest askRequest) {
        Long userId = SecurityUtils.getLoginUserId();
        return ragService.askStream(askRequest.getQuestion(), askRequest.getKnowledgeBaseId(), userId,
                askRequest.getTopK(), askRequest.getConversationId(), askRequest.getEnableWebSearchFallback());
    }

    /**
     * 分页获取RAG问答历史
     *
     * @param queryRequest 查询请求
     * @param request      HTTP 请求
     * @return RAG历史分页列表
     */
    @PostMapping("/history/list/page/vo")
    @Operation(summary = "分页获取RAG历史", description = "分页获取当前用户的RAG问答历史记录")
    public BaseResponse<Page<RAGHistoryVO>> listRAGHistoryVOByPage(@RequestBody RAGHistoryQueryRequest queryRequest,
                                                                  HttpServletRequest request) {
        Long userId = SecurityUtils.getLoginUserId();
        queryRequest.setUserId(userId);
        Page<RAGHistoryVO> page = ragService.listRAGHistoryVOByPage(queryRequest, request);
        return ResultUtils.success(page);
    }

    /**
     * 召回效果分析
     * <p>
     * 分析给定问题的召回效果，评估检索到的知识片段的相关性。
     *
     * @param request 分析请求
     * @return 召回分析结果
     */
    @PostMapping("/recall/analyze")
    @Operation(summary = "召回效果分析", description = "分析给定问题的召回效果，评估检索到的知识片段的相关性")
    @OperationLog(module = "RAG问答", action = "召回效果分析")
    public BaseResponse<RecallAnalysisVO> analyzeRecall(@RequestBody RecallAnalysisRequest request) {
        RecallAnalysisVO analysis = ragService.analyzeRecall(request);
        return ResultUtils.success(analysis);
    }

    /**
     * 批量召回效果分析
     * <p>
     * 批量分析多个问题的召回效果，用于评估检索系统的整体性能。
     *
     * @param request 批量分析请求
     * @return 批量召回分析结果
     */
    @PostMapping("/recall/batch/analyze")
    @Operation(summary = "批量召回效果分析", description = "批量分析多个问题的召回效果，用于评估检索系统的整体性能")
    @OperationLog(module = "RAG问答", action = "批量召回效果分析")
    public BaseResponse<BatchRecallVO> batchAnalyzeRecall(@RequestBody BatchRecallRequest request) {
        BatchRecallVO analysis = ragService.batchAnalyzeRecall(request);
        return ResultUtils.success(analysis);
    }
}
