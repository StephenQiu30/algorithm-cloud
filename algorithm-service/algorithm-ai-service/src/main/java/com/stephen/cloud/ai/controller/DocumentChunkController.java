package com.stephen.cloud.ai.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.service.DocumentChunkService;
import com.stephen.cloud.api.knowledge.model.dto.chunk.DocumentChunkQueryRequest;
import com.stephen.cloud.common.common.*;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 文档分片管理接口（管理员调试用）
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/ai/chunk")
@Tag(name = "DocumentChunkController", description = "文档分片管理接口（管理员调试）")
@Slf4j
public class DocumentChunkController {

    @Resource
    private DocumentChunkService documentChunkService;

    /**
     * 根据 ID 获取文档分片详情（管理员调试用）
     *
     * @param id 分片 ID
     * @return 文档分片实体
     */
    @GetMapping("/get/vo")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "获取文档分片详情", description = "管理员查看分片内容，用于调试和质量检查。")
    public BaseResponse<DocumentChunk> getById(@Parameter(description = "分片 ID") @RequestParam("id") long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        DocumentChunk chunk = documentChunkService.getById(id);
        ThrowUtils.throwIf(chunk == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(chunk);
    }

    /**
     * 删除指定文档分片（管理员调试用，慎用）
     *
     * @param deleteRequest 包含分片 ID 的请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    @Operation(summary = "删除分片", description = "管理员手动删除指定分片，通常用于清理异常数据。")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "文档分片模块", action = "删除分片")
    public BaseResponse<Boolean> deleteDocumentChunk(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(documentChunkService.removeById(deleteRequest.getId()));
    }

    /**
     * 删除指定文档下的所有分片（管理员维护用）
     *
     * @param deleteRequest 包含文档 ID 的请求
     * @return 是否成功
     */
    @PostMapping("/delete/by/document")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "按文档删除分片", description = "管理员批量删除特定文档的所有分片，用于数据清理。")
    public BaseResponse<Boolean> deleteByDocumentId(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(documentChunkService.deleteByDocumentId(deleteRequest.getId()));
    }

    /**
     * 分页查询文档分片列表（管理员调试用）
     *
     * @param queryRequest 分页查询请求
     * @return 分片分页结果
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "分页获取文档分片", description = "管理员查看分片列表，用于质量检查和调试。")
    public BaseResponse<Page<DocumentChunk>> listDocumentChunkByPage(@RequestBody DocumentChunkQueryRequest queryRequest) {
        if (queryRequest == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        Page<DocumentChunk> page = documentChunkService.page(new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize()),
                documentChunkService.getQueryWrapper(queryRequest));
        return ResultUtils.success(page);
    }
}
