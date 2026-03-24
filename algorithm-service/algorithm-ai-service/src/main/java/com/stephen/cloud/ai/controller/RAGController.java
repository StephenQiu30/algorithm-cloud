package com.stephen.cloud.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.service.RAGService;
import com.stephen.cloud.api.ai.model.dto.rag.RAGAskRequest;
import com.stephen.cloud.api.ai.model.dto.rag.RAGHistoryQueryRequest;
import com.stephen.cloud.api.ai.model.vo.RAGHistoryVO;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
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
    public BaseResponse<Page<RAGHistoryVO>> listHistoryByPage(@RequestBody RAGHistoryQueryRequest queryRequest) {
        Long userId = SecurityUtils.getLoginUserId();
        Page<RAGHistoryVO> page = ragService.listHistoryByPage(queryRequest.getCurrent(), queryRequest.getPageSize(),
                queryRequest.getKnowledgeBaseId(), userId);
        return ResultUtils.success(page);
    }
}
