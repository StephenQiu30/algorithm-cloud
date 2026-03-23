package com.stephen.cloud.ai.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.convert.KnowledgeDocumentConvert;
import com.stephen.cloud.ai.model.entity.KnowledgeDocument;
import com.stephen.cloud.ai.service.KnowledgeBaseService;
import com.stephen.cloud.ai.service.KnowledgeDocumentService;
import com.stephen.cloud.ai.service.KnowledgeIngestService;
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库文档管理接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/ai/knowledge/document")
@Slf4j
@Tag(name = "KnowledgeDocumentController", description = "知识库文档管理接口")
public class KnowledgeDocumentController {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;
    @Resource
    private KnowledgeIngestService knowledgeIngestService;

    /**
     * 上传知识库文档并触发自动解析
     *
     * @param knowledgeBaseId 关联的知识库 ID
     * @param file            二进制文档文件
     * @return 新建的文档 ID
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "上传文档",
            description = "上传知识库文档并触发异步解析。")
    @OperationLog(module = "知识库文档模块", action = "上传知识库文档")
    public BaseResponse<Long> uploadDocument(
            @Parameter(description = "关联的知识库 ID", required = true) @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
            @RequestPart("file")
            @Parameter(
                    description = "待上传的文档文件",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
            MultipartFile file) {
        ThrowUtils.throwIf(knowledgeBaseId == null || knowledgeBaseId <= 0, ErrorCode.PARAMS_ERROR, "知识库 ID 无效");
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        Long userId = SecurityUtils.getLoginUserId();
        Long documentId = knowledgeBaseService.uploadDocument(knowledgeBaseId, file, userId);
        return ResultUtils.success(documentId);
    }

    @PostMapping("/retry")
    @Operation(summary = "重试文档解析", description = "将失败或待处理文档重新投递到异步入库队列。")
    @OperationLog(module = "知识库文档模块", action = "重试文档解析")
    public BaseResponse<Boolean> retryIngest(@Parameter(description = "文档 ID", required = true) @RequestParam("documentId") Long documentId) {
        ThrowUtils.throwIf(documentId == null || documentId <= 0, ErrorCode.PARAMS_ERROR, "文档 ID 非法");
        knowledgeIngestService.retryIngest(documentId);
        return ResultUtils.success(true);
    }

    @GetMapping("/status")
    @Operation(summary = "查询文档解析状态", description = "用于上传后轮询查看解析状态与失败原因。")
    public BaseResponse<KnowledgeDocumentVO> getIngestStatus(@Parameter(description = "文档 ID", required = true) @RequestParam("documentId") Long documentId) {
        ThrowUtils.throwIf(documentId == null || documentId <= 0, ErrorCode.PARAMS_ERROR, "文档 ID 非法");
        KnowledgeDocument doc = knowledgeDocumentService.getById(documentId);
        ThrowUtils.throwIf(doc == null, ErrorCode.NOT_FOUND_ERROR, "文档不存在");
        return ResultUtils.success(knowledgeDocumentService.getKnowledgeDocumentVO(doc));
    }

    /**
     * 删除知识库文档及其关联记录 (分片、向量等)
     *
     * @param deleteRequest 包含文档 ID 的请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    @Operation(summary = "删除知识库文档", description = "同步删除文档记录及其在向量库、数据库关联的所有分片数据。")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "知识库文档模块", action = "删除知识库文档")
    public BaseResponse<Boolean> deleteDocument(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = SecurityUtils.getLoginUserId();
        boolean result = knowledgeBaseService.deleteDocumentAndAssociated(deleteRequest.getId(), userId);
        return ResultUtils.success(result);
    }

    /**
     * 更新文档基本信息 (管理员)
     *
     * @param updateRequest 更新请求参数
     * @return 是否成功
     */
    @PostMapping("/update")
    @Operation(summary = "更新知识库文档(管理员)", description = "管理员强制覆盖更新文档的基本元数据。")
    @OperationLog(module = "知识库文档模块", action = "更新知识库文档(管理员)")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateDocument(@RequestBody KnowledgeDocumentUpdateRequest updateRequest) {
        if (updateRequest == null || updateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        KnowledgeDocument doc = KnowledgeDocumentConvert.INSTANCE.documentUpdateRequestToObj(updateRequest);
        knowledgeDocumentService.validKnowledgeDocument(doc, false);
        ThrowUtils.throwIf(knowledgeDocumentService.getById(updateRequest.getId()) == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(knowledgeDocumentService.updateById(doc));
    }

    /**
     * 编辑文档信息 (用户本人)
     *
     * @param editRequest 编辑请求参数
     * @return 是否成功
     */
    @PostMapping("/edit")
    @Operation(summary = "编辑知识库文档", description = "编辑文档详情，仅本人或管理员可操作。")
    @OperationLog(module = "知识库文档模块", action = "编辑知识库文档")
    public BaseResponse<Boolean> editDocument(@RequestBody KnowledgeDocumentEditRequest editRequest) {
        if (editRequest == null || editRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        KnowledgeDocument doc = KnowledgeDocumentConvert.INSTANCE.documentEditRequestToObj(editRequest);
        knowledgeDocumentService.validKnowledgeDocument(doc, false);
        ThrowUtils.throwIf(knowledgeDocumentService.getById(editRequest.getId()) == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(knowledgeDocumentService.updateById(doc));
    }

    /**
     * 根据 ID 获取文档详情 (脱敏)
     *
     * @param id 文档 ID
     * @return 文档视图对象
     */
    @GetMapping("/get/vo")
    @Operation(summary = "获取文档详情", description = "根据 ID 获取单个知识库文档的详细视图信息。")
    public BaseResponse<KnowledgeDocumentVO> getDocumentVOById(@Parameter(description = "文档 ID") @RequestParam("id") Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        KnowledgeDocument doc = knowledgeDocumentService.getById(id);
        ThrowUtils.throwIf(doc == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(knowledgeDocumentService.getKnowledgeDocumentVO(doc));
    }

    /**
     * 分页查询知识库文档列表
     *
     * @param queryRequest 分页查询请求
     * @return 文档视图列表分页结果
     */
    @PostMapping("/list/page/vo")
    @Operation(summary = "分页获取知识库文档", description = "管理员视角分页查询所有文档记录及解析状态。")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<KnowledgeDocumentVO>> listDocumentVOByPage(@RequestBody KnowledgeDocumentQueryRequest queryRequest) {
        if (queryRequest == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        Page<KnowledgeDocument> page = knowledgeDocumentService.page(new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize()),
                knowledgeDocumentService.getQueryWrapper(queryRequest));
        return ResultUtils.success(knowledgeDocumentService.getKnowledgeDocumentVOPage(page));
    }
}
