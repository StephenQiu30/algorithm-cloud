package com.stephen.cloud.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.model.entity.Document;
import com.stephen.cloud.ai.service.DocumentService;
import com.stephen.cloud.api.ai.model.dto.document.DocumentQueryRequest;
import com.stephen.cloud.api.ai.model.vo.DocumentVO;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.*;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/doc")
@Tag(name = "DocumentController", description = "文档管理")
public class DocumentController {

    @Resource
    private DocumentService documentService;

    @PostMapping("/add")
    @Operation(summary = "上传文档")
    @OperationLog(module = "文档管理", action = "上传文档")
    public BaseResponse<Long> addDocument(@RequestParam("file") MultipartFile file,
                                          @RequestParam("knowledgeBaseId") Long knowledgeBaseId) {
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

    @PostMapping("/list/page/vo")
    @Operation(summary = "分页获取文档")
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

    @PostMapping("/my/list/page/vo")
    @Operation(summary = "分页获取我的文档")
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
