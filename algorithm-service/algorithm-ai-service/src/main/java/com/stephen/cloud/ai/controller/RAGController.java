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

@RestController
@RequestMapping("/ai/rag")
@Tag(name = "RAGController", description = "RAG问答")
public class RAGController {

    @Resource
    private RAGService ragService;

    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "RAG流式问答")
    @OperationLog(module = "RAG问答", action = "RAG流式问答")
    public Flux<String> askStream(@RequestBody RAGAskRequest askRequest) {
        Long userId = SecurityUtils.getLoginUserId();
        return ragService.askStream(askRequest.getQuestion(), askRequest.getKnowledgeBaseId(), userId,
                askRequest.getTopK());
    }

    @PostMapping("/history/list/page/vo")
    @Operation(summary = "分页获取RAG历史")
    public BaseResponse<Page<RAGHistoryVO>> listRAGHistoryVOByPage(@RequestBody RAGHistoryQueryRequest queryRequest,
                                                                  HttpServletRequest request) {
        Long userId = SecurityUtils.getLoginUserId();
        queryRequest.setUserId(userId);
        Page<RAGHistoryVO> page = ragService.listRAGHistoryVOByPage(queryRequest, request);
        return ResultUtils.success(page);
    }

    @PostMapping("/recall/analyze")
    @Operation(summary = "召回效果分析")
    @OperationLog(module = "RAG问答", action = "召回效果分析")
    public BaseResponse<RecallAnalysisVO> analyzeRecall(@RequestBody RecallAnalysisRequest request) {
        RecallAnalysisVO analysis = ragService.analyzeRecall(request);
        return ResultUtils.success(analysis);
    }

    @PostMapping("/recall/batch/analyze")
    @Operation(summary = "批量召回效果分析")
    @OperationLog(module = "RAG问答", action = "批量召回效果分析")
    public BaseResponse<BatchRecallVO> batchAnalyzeRecall(@RequestBody BatchRecallRequest request) {
        BatchRecallVO analysis = ragService.batchAnalyzeRecall(request);
        return ResultUtils.success(analysis);
    }
}
