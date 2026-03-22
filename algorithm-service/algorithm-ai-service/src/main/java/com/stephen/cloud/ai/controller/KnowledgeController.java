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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库接口
 * <p>
 * 提供知识库的生命周期管理（CURD）、文档上传及基于库的 RAG 对话能力。
 * </p>
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/knowledge")
@Slf4j
@Tag(name = "KnowledgeController", description = "知识库管理及 RAG 接口")
public class KnowledgeController {

    @Resource
    private KnowledgeService knowledgeService;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Resource
    private RagService ragService;

    /**
     * 创建知识库
     *
     * @param addRequest 创建参数
     * @param request    请求对象
     * @return 新知识库 ID
     */
    @PostMapping("/add")
    @Operation(summary = "创建知识库")
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

    /**
     * 删除知识库
     *
     * @param deleteRequest 包含 ID 的删除请求
     * @param request       请求对象
     * @return 是否成功
     */
    @PostMapping("/delete")
    @Operation(summary = "删除知识库")
    @OperationLog(module = "知识库模块", action = "删除知识库")
    public BaseResponse<Boolean> deleteKnowledgeBase(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = SecurityUtils.getLoginUserId();
        Long id = deleteRequest.getId();
        // 校验是否存在并有删除权限
        knowledgeService.getAndCheckAccess(id, userId);
        boolean b = knowledgeService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新知识库 (管理员权限)
     *
     * @param updateRequest 更新请求
     * @param request       请求对象
     * @return 是否成功
     */
    @PostMapping("/update")
    @Operation(summary = "管理员更新知识库")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "知识库模块", action = "管理员更新知识库")
    public BaseResponse<Boolean> updateKnowledgeBase(@RequestBody KnowledgeBaseUpdateRequest updateRequest, HttpServletRequest request) {
        if (updateRequest == null || updateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        KnowledgeBase knowledgeBase = KnowledgeConvert.updateRequestToObj(updateRequest);
        knowledgeService.validKnowledgeBase(knowledgeBase, false);
        boolean result = knowledgeService.updateById(knowledgeBase);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取知识库视图
     *
     * @param id 知识库 ID
     * @return 知识库视图
     */
    @GetMapping("/get/vo")
    @Operation(summary = "根据 id 获取知识库详情")
    public BaseResponse<KnowledgeBaseVO> getKnowledgeBaseVOById(Long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        KnowledgeBase knowledgeBase = knowledgeService.getById(id);
        if (knowledgeBase == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(knowledgeService.getKnowledgeBaseVO(knowledgeBase, request));
    }

    /**
     * 分页获取我的知识库列表
     *
     * @param queryRequest 分页查询请求
     * @param request      请求对象
     * @return 分页视图结果
     */
    @PostMapping("/my/list/page/vo")
    @Operation(summary = "分页获取我的知识库列表")
    public BaseResponse<Page<KnowledgeBaseVO>> listMyKnowledgeBaseVOByPage(@RequestBody KnowledgeBaseQueryRequest queryRequest, HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        queryRequest.setUserId(SecurityUtils.getLoginUserId());
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        // 限制爬虫或恶意大页查询
        if (size > 20) {
            size = 20;
        }
        Page<KnowledgeBase> knowledgeBasePage = knowledgeService.page(new Page<>(current, size),
                knowledgeService.getQueryWrapper(queryRequest));
        return ResultUtils.success(knowledgeService.getKnowledgeBaseVOPage(knowledgeBasePage, request));
    }

    /**
     * 知识库文档上传
     *
     * @param knowledgeBaseId 所属知识库 ID
     * @param file            二进制文件
     * @return 记录 ID
     */
    @PostMapping("/document/upload")
    @Operation(summary = "上传知识库文档")
    @OperationLog(module = "知识库模块", action = "上传文档到知识库")
    public BaseResponse<Long> uploadDocument(@RequestParam("knowledgeBaseId") Long knowledgeBaseId, @RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getLoginUserId();
        Long documentId = knowledgeDocumentService.uploadDocument(knowledgeBaseId, file, userId);
        return ResultUtils.success(documentId);
    }

    /**
     * 发起 RAG 对话提问
     *
     * @param knowledgeBaseId 对应知识库 ID
     * @param request         问答内容
     * @return RAG 完整答复视图 (含源切片)
     */
    @PostMapping("/chat/{knowledgeBaseId}")
    @Operation(summary = "发起 RAG 知识库问答")
    public BaseResponse<RagChatResponseVO> ragChat(@PathVariable Long knowledgeBaseId, @RequestBody RagChatRequest request) {
        Long userId = SecurityUtils.getLoginUserId();
        RagChatResponseVO response = ragService.ragChat(knowledgeBaseId, request, userId);
        return ResultUtils.success(response);
    }
}
