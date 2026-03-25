package com.stephen.cloud.ai.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.model.entity.Document;
import com.stephen.cloud.ai.service.DocumentService;
import com.stephen.cloud.api.ai.model.dto.document.DocumentQueryRequest;
import com.stephen.cloud.api.ai.model.vo.DocumentVO;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.*;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档管理接口
 * <p>
 * 提供知识库文档的上传、删除、查询等功能。
 * 文档上传后将自动触发 ETL 处理流程，完成解析、分片、向量化并入库。
 * </p>
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/ai/doc")
@Tag(name = "DocumentController", description = "知识库文档管理")
public class DocumentController {

    @Resource
    private DocumentService documentService;

    /**
     * 上传文档
     * <p>
     * 上传文档到指定知识库，系统将自动进行文档解析、分片和向量化处理。
     *
     * @param file           上传的文件
     * @param knowledgeBaseId 知识库 ID
     * @return 新创建的文档 ID
     */
    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文档", description = "上传文档到指定知识库，系统将自动进行文档解析、分片和向量化处理")
    @OperationLog(module = "文档管理", action = "上传文档")
    public BaseResponse<Long> addDocument(
            @Parameter(
                    description = "上传文件",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary"))
            )
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "知识库 ID", required = true, example = "1")
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId) {
        ThrowUtils.throwIf(!SecurityUtils.isAdmin(), ErrorCode.NO_AUTH_ERROR);
        Long documentId = documentService.uploadDocument(file, knowledgeBaseId, SecurityUtils.getLoginUserId());
        return ResultUtils.success(documentId);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除文档")
    @OperationLog(module = "文档管理", action = "删除文档")
    public BaseResponse<Boolean> deleteDocument(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(documentService.deleteDocumentById(
                deleteRequest.getId(),
                SecurityUtils.getLoginUserId(),
                SecurityUtils.isAdmin()
        ));
    }

    @GetMapping("/get/vo")
    @Operation(summary = "获取文档详情")
    public BaseResponse<DocumentVO> getDocumentVOById(@RequestParam("id") long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Document document = documentService.getById(id);
        ThrowUtils.throwIf(document == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(documentService.getDocumentVO(document, request));
    }

    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "分页获取文档（管理员）")
    public BaseResponse<Page<Document>> listDocumentByPage(@RequestBody DocumentQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<Document> page = documentService.page(new Page<>(current, size),
                documentService.getQueryWrapper(queryRequest));
        return ResultUtils.success(page);
    }

    /**
     * 分页获取文档列表（封装类）
     *
     * @param queryRequest 查询请求
     * @param request      HTTP 请求
     * @return 文档 VO 分页列表
     */
    @PostMapping("/list/page/vo")
    @Operation(summary = "分页获取文档", description = "分页获取文档脱敏信息列表")
    public BaseResponse<Page<DocumentVO>> listDocumentVOByPage(@RequestBody DocumentQueryRequest queryRequest,
                                                                HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<Document> page = documentService.page(new Page<>(current, size),
                documentService.getQueryWrapper(queryRequest));
        return ResultUtils.success(documentService.getDocumentVOPage(page, request));
    }

    /**
     * 我的文档列表
     * <p>
     * 分页获取当前登录用户上传的文档列表。
     *
     * @param queryRequest 查询请求
     * @param request      HTTP 请求
     * @return 用户的文档 VO 分页列表
     */
    @PostMapping("/my/list/page/vo")
    @Operation(summary = "我的文档列表", description = "分页获取当前登录用户上传的文档列表")
    public BaseResponse<Page<DocumentVO>> listMyDocumentVOByPage(@RequestBody DocumentQueryRequest queryRequest,
                                                                  HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        queryRequest.setUserId(SecurityUtils.getLoginUserId());
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<Document> page = documentService.page(new Page<>(current, size),
                documentService.getQueryWrapper(queryRequest));
        return ResultUtils.success(documentService.getDocumentVOPage(page, request));
    }
}
