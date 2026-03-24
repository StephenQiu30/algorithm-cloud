package com.stephen.cloud.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.service.ChunkService;
import com.stephen.cloud.api.ai.model.dto.chunk.ChunkQueryRequest;
import com.stephen.cloud.api.ai.model.dto.chunk.ChunkSearchRequest;
import com.stephen.cloud.api.ai.model.vo.ChunkVO;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/chunk")
@Tag(name = "ChunkController", description = "文档分片管理")
public class ChunkController {

    @Resource
    private ChunkService chunkService;

    @PostMapping("/list/page/vo")
    @Operation(summary = "分页查询文档分片")
    @OperationLog(module = "文档分片管理", action = "分页查询文档分片")
    public BaseResponse<Page<ChunkVO>> listChunkVOByPage(@RequestBody ChunkQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ThrowUtils.throwIf(queryRequest.getDocumentId() == null || queryRequest.getDocumentId() <= 0,
                ErrorCode.PARAMS_ERROR, "文档ID不能为空");
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<DocumentChunk> page = chunkService.page(new Page<>(current, size),
                chunkService.getQueryWrapper(queryRequest));
        return ResultUtils.success(chunkService.getChunkVOPage(page));
    }

    @GetMapping("/get/vo")
    @Operation(summary = "获取分片详情")
    @OperationLog(module = "文档分片管理", action = "获取分片详情")
    public BaseResponse<ChunkVO> getChunkVOById(@RequestParam("id") long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        DocumentChunk chunk = chunkService.getById(id);
        ThrowUtils.throwIf(chunk == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(chunkService.getChunkVO(chunk));
    }

    @PostMapping("/search")
    @Operation(summary = "内容检索分片")
    @OperationLog(module = "文档分片管理", action = "内容检索分片")
    public BaseResponse<List<ChunkVO>> searchChunks(@RequestBody ChunkSearchRequest searchRequest) {
        if (searchRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ThrowUtils.throwIf(StringUtils.isBlank(searchRequest.getQuery()),
                ErrorCode.PARAMS_ERROR, "检索内容不能为空");
        return ResultUtils.success(chunkService.searchChunks(searchRequest));
    }
}
