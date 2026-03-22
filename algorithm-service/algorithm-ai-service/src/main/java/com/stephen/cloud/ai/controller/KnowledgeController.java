package com.stephen.cloud.ai.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.convert.KnowledgeConvert;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.ai.service.KnowledgeDocumentService;
import com.stephen.cloud.ai.service.KnowledgeService;
import com.stephen.cloud.ai.service.RagService;
import com.stephen.cloud.api.knowledge.model.dto.*;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeBaseVO;
import com.stephen.cloud.api.knowledge.model.vo.RagChatResponseVO;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.DeleteRequest;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/knowledge")
@Slf4j
@Tag(name = "KnowledgeController", description = "知识库与 RAG")
public class KnowledgeController {

    @Resource
    private KnowledgeService knowledgeService;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Resource
    private RagService ragService;

    @PostMapping("/add")
    @Operation(summary = "创建知识库")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "知识库模块", action = "创建知识库")
    public BaseResponse<Long> addKnowledgeBase(@RequestBody KnowledgeBaseAddRequest addRequest, HttpServletRequest request) {
        if (addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        KnowledgeBase knowledgeBase = KnowledgeConvert.addRequestToObj(addRequest);
        knowledgeBase.setUserId(SecurityUtils.getLoginUserId());
        knowledgeService.validKnowledgeBase(knowledgeBase, true);
        boolean result = knowledgeService.save(knowledgeBase);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(knowledgeBase.getId());
    }

    @PostMapping("/delete")
    @Operation(summary = "删除知识库")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "知识库模块", action = "删除知识库")
    public BaseResponse<Boolean> deleteKnowledgeBase(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = deleteRequest.getId();
        if (knowledgeService.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        }
        boolean b = knowledgeService.removeById(id);
        return ResultUtils.success(b);
    }

    @PostMapping("/update")
    @Operation(summary = "管理员更新知识库")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "知识库模块", action = "管理员更新知识库")
    public BaseResponse<Boolean> updateKnowledgeBase(@RequestBody KnowledgeBaseUpdateRequest updateRequest, HttpServletRequest request) {
        if (updateRequest == null || updateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (knowledgeService.getById(updateRequest.getId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        }
        KnowledgeBase knowledgeBase = KnowledgeConvert.updateRequestToObj(updateRequest);
        knowledgeService.validKnowledgeBase(knowledgeBase, false);
        boolean result = knowledgeService.updateById(knowledgeBase);
        return ResultUtils.success(result);
    }

    @GetMapping("/get/vo")
    @Operation(summary = "根据 id 获取知识库详情")
    public BaseResponse<KnowledgeBaseVO> getKnowledgeBaseVOById(Long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        KnowledgeBase knowledgeBase = knowledgeService.getById(id);
        if (knowledgeBase == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        }
        return ResultUtils.success(knowledgeService.getKnowledgeBaseVO(knowledgeBase, request));
    }

    @PostMapping("/my/list/page/vo")
    @Operation(summary = "分页获取知识库列表")
    public BaseResponse<Page<KnowledgeBaseVO>> listMyKnowledgeBaseVOByPage(@RequestBody KnowledgeBaseQueryRequest queryRequest, HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        if (size > 20) {
            size = 20;
        }
        Page<KnowledgeBase> knowledgeBasePage = knowledgeService.page(new Page<>(current, size),
                knowledgeService.getQueryWrapper(queryRequest));
        return ResultUtils.success(knowledgeService.getKnowledgeBaseVOPage(knowledgeBasePage, request));
    }

    @PostMapping(value = "/document/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传知识库文档")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "知识库模块", action = "上传文档到知识库")
    public BaseResponse<Long> uploadDocument(
            @Parameter(description = "所属知识库 ID") @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
            @Parameter(description = "二进制文件") @RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getLoginUserId();
        Long documentId = knowledgeDocumentService.uploadDocument(knowledgeBaseId, file, userId);
        return ResultUtils.success(documentId);
    }

    @PostMapping("/document/delete")
    @Operation(summary = "删除知识库文档")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "知识库模块", action = "删除知识库文档")
    public BaseResponse<Boolean> deleteDocument(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = SecurityUtils.getLoginUserId();
        boolean result = knowledgeDocumentService.deleteDocument(deleteRequest.getId(), userId);
        return ResultUtils.success(result);
    }

    @PostMapping("/chat")
    @Operation(summary = "发起 RAG 知识库问答")
    @OperationLog(module = "知识库模块", action = "发起 RAG 问答")
    public BaseResponse<RagChatResponseVO> ragChat(@RequestBody RagChatRequest request) {
        Long userId = SecurityUtils.getLoginUserId();
        RagChatResponseVO response = ragService.ragChat(request, userId);
        return ResultUtils.success(response);
    }
}
