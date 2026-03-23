package com.stephen.cloud.ai.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.convert.DocumentChunkConvert;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.service.DocumentChunkService;
import com.stephen.cloud.api.knowledge.model.dto.chunk.DocumentChunkAddRequest;
import com.stephen.cloud.api.knowledge.model.dto.chunk.DocumentChunkQueryRequest;
import com.stephen.cloud.common.common.*;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.log.annotation.OperationLog;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 文档分片接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/ai/chunk")
@Tag(name = "DocumentChunkController", description = "文档分片管理接口")
@Slf4j
public class DocumentChunkController {

    @Resource
    private DocumentChunkService documentChunkService;

    @GetMapping("/get/vo")
    @Operation(summary = "获取文档分片详情")
    public BaseResponse<DocumentChunk> getById(@RequestParam("id") long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        DocumentChunk chunk = documentChunkService.getById(id);
        ThrowUtils.throwIf(chunk == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(chunk);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除分片")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "文档分片模块", action = "删除分片")
    public BaseResponse<Boolean> deleteDocumentChunk(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = deleteRequest.getId();
        DocumentChunk old = documentChunkService.getById(id);
        ThrowUtils.throwIf(old == null, ErrorCode.NOT_FOUND_ERROR);
        boolean ok = documentChunkService.removeById(id);
        ThrowUtils.throwIf(!ok, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/delete/by/document")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "按文档删除分片")
    public BaseResponse<Boolean> deleteByDocumentId(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = deleteRequest.getId();
        boolean ok = documentChunkService.deleteByDocumentId(id);
        return ResultUtils.success(ok);
    }

    @PostMapping("/add")
    @Operation(summary = "创建分片")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "文档分片模块", action = "创建分片")
    public BaseResponse<Long> addDocumentChunk(@RequestBody DocumentChunkAddRequest addRequest) {
        if (addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DocumentChunk documentChunk = DocumentChunkConvert.INSTANCE.addRequestToObj(addRequest);
        boolean result = documentChunkService.save(documentChunk);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(documentChunk.getId());
    }

    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "分页获取文档分片")
    public BaseResponse<Page<DocumentChunk>> listByPage(@RequestBody DocumentChunkQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        LambdaQueryWrapper<DocumentChunk> qw = new LambdaQueryWrapper<>();
        Long documentId = queryRequest.getDocumentId();
        Long knowledgeBaseId = queryRequest.getKnowledgeBaseId();
        Integer chunkIndex = queryRequest.getChunkIndex();

        qw.eq(documentId != null && documentId > 0, DocumentChunk::getDocumentId, documentId);
        qw.eq(knowledgeBaseId != null && knowledgeBaseId > 0, DocumentChunk::getKnowledgeBaseId, knowledgeBaseId);
        qw.eq(chunkIndex != null, DocumentChunk::getChunkIndex, chunkIndex);

        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();
        boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);

        if (SqlUtils.validSortField(sortField)) {
            switch (sortField) {
                case "createTime" -> qw.orderBy(true, isAsc, DocumentChunk::getCreateTime);
                case "chunkIndex" -> qw.orderBy(true, isAsc, DocumentChunk::getChunkIndex);
                case "tokenEstimate" -> qw.orderBy(true, isAsc, DocumentChunk::getTokenEstimate);
                default -> qw.orderByDesc(DocumentChunk::getCreateTime);
            }
        } else {
            qw.orderByDesc(DocumentChunk::getCreateTime);
        }

        Page<DocumentChunk> page = documentChunkService.page(new Page<>(current, size), qw);
        return ResultUtils.success(page);
    }
}
