package com.stephen.cloud.ai.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.convert.DocumentChunkConvert;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.service.DocumentChunkService;
import com.stephen.cloud.api.knowledge.model.dto.chunk.DocumentChunkAddRequest;
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
 * 文档分片管理接口
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

    /**
     * 根据 ID 获取文档分片详情
     *
     * @param id 分片 ID
     * @return 文档分片实体
     */
    @GetMapping("/get/vo")
    @Operation(summary = "获取文档分片详情", description = "根据主键 ID 查询分片的内容及元数据。")
    public BaseResponse<DocumentChunk> getById(@Parameter(description = "分片 ID") @RequestParam("id") long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        DocumentChunk chunk = documentChunkService.getById(id);
        ThrowUtils.throwIf(chunk == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(chunk);
    }

    /**
     * 删除指定文档分片
     *
     * @param deleteRequest 包含分片 ID 的请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    @Operation(summary = "删除分片", description = "手动删除指定的文本分片记录。")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "文档分片模块", action = "删除分片")
    public BaseResponse<Boolean> deleteDocumentChunk(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(documentChunkService.removeById(deleteRequest.getId()));
    }

    /**
     * 删除指定文档下的所有分片
     *
     * @param deleteRequest 包含文档 ID 的请求
     * @return 是否成功
     */
    @PostMapping("/delete/by/document")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "按文档删除分片", description = "删除特定文档下的所有分片记录。")
    public BaseResponse<Boolean> deleteByDocumentId(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(documentChunkService.deleteByDocumentId(deleteRequest.getId()));
    }

    /**
     * 手动创建文档分片
     *
     * @param addRequest 创建请求
     * @return 新建分片 ID
     */
    @PostMapping("/add")
    @Operation(summary = "创建分片", description = "手动新增一个文本分片（通常由后台或离线脚本使用）。")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "文档分片模块", action = "创建分片")
    public BaseResponse<Long> addDocumentChunk(@RequestBody DocumentChunkAddRequest addRequest) {
        if (addRequest == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        DocumentChunk documentChunk = DocumentChunkConvert.INSTANCE.addRequestToObj(addRequest);
        boolean result = documentChunkService.save(documentChunk);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(documentChunk.getId());
    }

    /**
     * 分页查询文档分片列表
     *
     * @param queryRequest 分页查询请求
     * @return 分片分页结果
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "分页获取文档分片", description = "管理员视角分页检索所有分块，支持完整字段返回。")
    public BaseResponse<Page<DocumentChunk>> listDocumentChunkByPage(@RequestBody DocumentChunkQueryRequest queryRequest) {
        if (queryRequest == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        Page<DocumentChunk> page = documentChunkService.page(new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize()),
                documentChunkService.getQueryWrapper(queryRequest));
        return ResultUtils.success(page);
    }
}
