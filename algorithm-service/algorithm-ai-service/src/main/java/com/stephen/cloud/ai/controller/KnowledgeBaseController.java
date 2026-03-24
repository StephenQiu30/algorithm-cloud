package com.stephen.cloud.ai.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.convert.KnowledgeBaseConvert;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.ai.service.DocumentService;
import com.stephen.cloud.ai.service.KnowledgeBaseService;
import com.stephen.cloud.api.ai.model.dto.document.DocumentQueryRequest;
import com.stephen.cloud.api.ai.model.dto.knowledgebase.KnowledgeBaseAddRequest;
import com.stephen.cloud.api.ai.model.dto.knowledgebase.KnowledgeBaseEditRequest;
import com.stephen.cloud.api.ai.model.dto.knowledgebase.KnowledgeBaseQueryRequest;
import com.stephen.cloud.api.ai.model.dto.knowledgebase.KnowledgeBaseUpdateRequest;
import com.stephen.cloud.api.ai.model.vo.KnowledgeBaseVO;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.*;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/kb")
@Tag(name = "KnowledgeBaseController", description = "知识库管理")
public class KnowledgeBaseController {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private DocumentService documentService;

    @PostMapping("/add")
    @Operation(summary = "创建知识库")
    @OperationLog(module = "知识库管理", action = "创建知识库")
    public BaseResponse<Long> addKnowledgeBase(@RequestBody KnowledgeBaseAddRequest addRequest) {
        KnowledgeBase knowledgeBase = KnowledgeBaseConvert.INSTANCE.addRequestToObj(addRequest);
        knowledgeBaseService.validKnowledgeBase(knowledgeBase, true);
        ThrowUtils.throwIf(!knowledgeBaseService.isNameUnique(knowledgeBase.getName(), null), ErrorCode.OPERATION_ERROR, "知识库名称已存在");
        knowledgeBase.setUserId(SecurityUtils.getLoginUserId());
        knowledgeBase.setDocumentCount(0);
        boolean result = knowledgeBaseService.save(knowledgeBase);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(knowledgeBase.getId());
    }

    @PostMapping("/delete")
    @Operation(summary = "删除知识库")
    @OperationLog(module = "知识库管理", action = "删除知识库")
    public BaseResponse<Boolean> deleteKnowledgeBase(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = deleteRequest.getId();
        KnowledgeBase oldKnowledgeBase = knowledgeBaseService.getById(id);
        ThrowUtils.throwIf(oldKnowledgeBase == null, ErrorCode.NOT_FOUND_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        ThrowUtils.throwIf(!oldKnowledgeBase.getUserId().equals(userId) && !SecurityUtils.isAdmin(), ErrorCode.NO_AUTH_ERROR);
        documentService.remove(new LambdaQueryWrapper<com.stephen.cloud.ai.model.entity.Document>()
                .eq(com.stephen.cloud.ai.model.entity.Document::getKnowledgeBaseId, id));
        boolean result = knowledgeBaseService.removeById(id);
        return ResultUtils.success(result);
    }

    @PostMapping("/update")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "管理员更新知识库")
    @OperationLog(module = "知识库管理", action = "管理员更新知识库")
    public BaseResponse<Boolean> updateKnowledgeBase(@RequestBody KnowledgeBaseUpdateRequest updateRequest) {
        KnowledgeBase knowledgeBase = KnowledgeBaseConvert.INSTANCE.updateRequestToObj(updateRequest);
        knowledgeBaseService.validKnowledgeBase(knowledgeBase, false);
        ThrowUtils.throwIf(knowledgeBaseService.getById(updateRequest.getId()) == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!knowledgeBaseService.isNameUnique(knowledgeBase.getName(), updateRequest.getId()),
                ErrorCode.OPERATION_ERROR, "知识库名称已存在");
        boolean result = knowledgeBaseService.updateById(knowledgeBase);
        return ResultUtils.success(result);
    }

    @PostMapping("/edit")
    @Operation(summary = "编辑知识库")
    @OperationLog(module = "知识库管理", action = "编辑知识库")
    public BaseResponse<Boolean> editKnowledgeBase(@RequestBody KnowledgeBaseEditRequest editRequest) {
        KnowledgeBase knowledgeBase = KnowledgeBaseConvert.INSTANCE.editRequestToObj(editRequest);
        knowledgeBaseService.validKnowledgeBase(knowledgeBase, false);
        KnowledgeBase oldKnowledgeBase = knowledgeBaseService.getById(editRequest.getId());
        ThrowUtils.throwIf(oldKnowledgeBase == null, ErrorCode.NOT_FOUND_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        ThrowUtils.throwIf(!oldKnowledgeBase.getUserId().equals(userId) && !SecurityUtils.isAdmin(), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(!knowledgeBaseService.isNameUnique(knowledgeBase.getName(), editRequest.getId()),
                ErrorCode.OPERATION_ERROR, "知识库名称已存在");
        boolean result = knowledgeBaseService.updateById(knowledgeBase);
        return ResultUtils.success(result);
    }

    @GetMapping("/get/vo")
    @Operation(summary = "获取知识库详情")
    public BaseResponse<KnowledgeBaseVO> getKnowledgeBaseVOById(@RequestParam("id") long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        KnowledgeBase knowledgeBase = knowledgeBaseService.getById(id);
        ThrowUtils.throwIf(knowledgeBase == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(knowledgeBaseService.getKnowledgeBaseVO(knowledgeBase, request));
    }

    @PostMapping("/list/page/vo")
    @Operation(summary = "分页获取知识库")
    public BaseResponse<Page<KnowledgeBaseVO>> listKnowledgeBaseVOByPage(@RequestBody KnowledgeBaseQueryRequest queryRequest,
                                                                          HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<KnowledgeBase> page = knowledgeBaseService.page(new Page<>(current, size),
                knowledgeBaseService.getQueryWrapper(queryRequest));
        return ResultUtils.success(knowledgeBaseService.getKnowledgeBaseVOPage(page, request));
    }

    @PostMapping("/my/list/page/vo")
    @Operation(summary = "分页获取我的知识库")
    public BaseResponse<Page<KnowledgeBaseVO>> listMyKnowledgeBaseVOByPage(@RequestBody KnowledgeBaseQueryRequest queryRequest,
                                                                            HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        queryRequest.setUserId(SecurityUtils.getLoginUserId());
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<KnowledgeBase> page = knowledgeBaseService.page(new Page<>(current, size),
                knowledgeBaseService.getQueryWrapper(queryRequest));
        return ResultUtils.success(knowledgeBaseService.getKnowledgeBaseVOPage(page, request));
    }
}
