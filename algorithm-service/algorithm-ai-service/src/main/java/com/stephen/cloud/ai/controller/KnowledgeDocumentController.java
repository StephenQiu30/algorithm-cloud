package com.stephen.cloud.ai.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.convert.KnowledgeDocumentConvert;
import com.stephen.cloud.ai.model.entity.KnowledgeDocument;
import com.stephen.cloud.ai.service.KnowledgeDocumentService;
import com.stephen.cloud.api.knowledge.model.dto.knowledgedocument.KnowledgeDocumentEditRequest;
import com.stephen.cloud.api.knowledge.model.dto.knowledgedocument.KnowledgeDocumentQueryRequest;
import com.stephen.cloud.api.knowledge.model.dto.knowledgedocument.KnowledgeDocumentUpdateRequest;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeDocumentVO;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库文档接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/ai/knowledge/document")
@Slf4j
@Tag(name = "KnowledgeDocumentController", description = "知识库文档管理接口")
public class KnowledgeDocumentController {

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    /**
     * 上传知识库文档
     *
     * @param knowledgeBaseId 所属知识库 ID
     * @param file            二进制文件
     * @return 文档 ID
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文档")
    @OperationLog(module = "知识库文档模块", action = "上传知识库文档")
    public BaseResponse<Long> uploadDocument(@RequestParam("knowledgeBaseId") Long knowledgeBaseId,
            @Parameter(description = "二进制文件") @RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getLoginUserId();
        Long documentId = knowledgeDocumentService.uploadDocument(knowledgeBaseId, file, userId);
        return ResultUtils.success(documentId);
    }

    /**
     * 删除知识库文档
     *
     * @param deleteRequest 删除请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    @Operation(summary = "删除知识库文档")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "知识库文档模块", action = "删除知识库文档")
    public BaseResponse<Boolean> deleteDocument(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = SecurityUtils.getLoginUserId();
        boolean result = knowledgeDocumentService.deleteDocument(deleteRequest.getId(), userId);
        return ResultUtils.success(result);
    }

    /**
     * 更新知识库文档 (管理员)
     *
     * @param updateRequest 更新请求
     * @return 是否成功
     */
    @PostMapping("/update")
    @Operation(summary = "更新知识库文档(管理员)")
    @OperationLog(module = "知识库文档模块", action = "更新知识库文档(管理员)")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateKnowledgeDocument(@RequestBody KnowledgeDocumentUpdateRequest updateRequest) {
        if (updateRequest == null || updateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        KnowledgeDocument knowledgeDocument = KnowledgeDocumentConvert.INSTANCE.documentUpdateRequestToObj(updateRequest);
        knowledgeDocumentService.validKnowledgeDocument(knowledgeDocument, false);
        long id = updateRequest.getId();
        KnowledgeDocument oldDocument = knowledgeDocumentService.getById(id);
        ThrowUtils.throwIf(oldDocument == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = knowledgeDocumentService.updateById(knowledgeDocument);
        return ResultUtils.success(result);
    }

    /**
     * 编辑知识库文档 (用户)
     *
     * @param editRequest 编辑请求
     * @return 是否成功
     */
    @PostMapping("/edit")
    @Operation(summary = "编辑知识库文档")
    @OperationLog(module = "知识库文档模块", action = "编辑知识库文档")
    public BaseResponse<Boolean> editKnowledgeDocument(@RequestBody KnowledgeDocumentEditRequest editRequest) {
        if (editRequest == null || editRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        KnowledgeDocument knowledgeDocument = KnowledgeDocumentConvert.INSTANCE.documentEditRequestToObj(editRequest);
        knowledgeDocumentService.validKnowledgeDocument(knowledgeDocument, false);
        long id = editRequest.getId();
        KnowledgeDocument oldDocument = knowledgeDocumentService.getById(id);
        ThrowUtils.throwIf(oldDocument == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = knowledgeDocumentService.updateById(knowledgeDocument);
        return ResultUtils.success(result);
    }

    /**
     * 分页查询知识库文档 (管理员)
     *
     * @param queryRequest 分页查询请求
     * @return 文档分页
     */
    @PostMapping("/list/page/vo")
    @Operation(summary = "分页获取知识库文档")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<KnowledgeDocumentVO>> listKnowledgeDocumentVOByPage(@RequestBody KnowledgeDocumentQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        // 限制查询页大小
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);
        Page<KnowledgeDocument> documentPage = knowledgeDocumentService.page(new Page<>(current, size),
                knowledgeDocumentService.getQueryWrapper(queryRequest));
        return ResultUtils.success(knowledgeDocumentService.getKnowledgeDocumentVOPage(documentPage));
    }
}
