package com.stephen.cloud.ai.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.convert.KnowledgeBaseConvert;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.ai.service.KnowledgeBaseService;
import com.stephen.cloud.ai.service.KnowledgeRetrievalService;
import com.stephen.cloud.api.knowledge.model.dto.retrieval.KnowledgeRetrievalRequest;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import com.stephen.cloud.api.knowledge.model.dto.knowledgebase.KnowledgeBaseAddRequest;
import com.stephen.cloud.api.knowledge.model.dto.knowledgebase.KnowledgeBaseQueryRequest;
import com.stephen.cloud.api.knowledge.model.dto.knowledgebase.KnowledgeBaseUpdateRequest;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeBaseVO;
import java.util.List;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.DeleteRequest;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.log.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/knowledge")
@Slf4j
@Tag(name = "KnowledgeBaseController", description = "知识库管理接口")
public class KnowledgeBaseController {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private KnowledgeRetrievalService knowledgeRetrievalService;



    /**
     * 创建知识库
     *
     * @param addRequest 创建请求
     * @param request    请求对象
     * @return 记录 ID
     */
    @PostMapping("/add")
    @Operation(summary = "创建知识库")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "知识库模块", action = "创建知识库")
    public BaseResponse<Long> addKnowledgeBase(@RequestBody KnowledgeBaseAddRequest addRequest,
            HttpServletRequest request) {
        if (addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        KnowledgeBase knowledgeBase = KnowledgeBaseConvert.INSTANCE.addRequestToObj(addRequest);
        knowledgeBase.setUserId(SecurityUtils.getLoginUserId());
        knowledgeBaseService.validKnowledgeBase(knowledgeBase, true);
        boolean result = knowledgeBaseService.save(knowledgeBase);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(knowledgeBase.getId());
    }

    /**
     * 删除知识库
     *
     * @param deleteRequest 删除请求
     * @param request       请求对象
     * @return 是否成功
     */
    @PostMapping("/delete")
    @Operation(summary = "删除知识库")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "知识库模块", action = "删除知识库")
    public BaseResponse<Boolean> deleteKnowledgeBase(@RequestBody DeleteRequest deleteRequest,
            HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = deleteRequest.getId();
        KnowledgeBase oldKnowledgeBase = knowledgeBaseService.getById(id);
        ThrowUtils.throwIf(oldKnowledgeBase == null, ErrorCode.NOT_FOUND_ERROR);
        boolean b = knowledgeBaseService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新知识库 (管理员)
     *
     * @param updateRequest 更新请求
     * @param request       请求对象
     * @return 是否成功
     */
    @PostMapping("/update")
    @Operation(summary = "管理员更新知识库")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "知识库模块", action = "管理员更新知识库")
    public BaseResponse<Boolean> updateKnowledgeBase(@RequestBody KnowledgeBaseUpdateRequest updateRequest,
            HttpServletRequest request) {
        if (updateRequest == null || updateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        KnowledgeBase oldKnowledgeBase = knowledgeBaseService.getById(updateRequest.getId());
        ThrowUtils.throwIf(oldKnowledgeBase == null, ErrorCode.NOT_FOUND_ERROR);
        KnowledgeBase knowledgeBase = KnowledgeBaseConvert.INSTANCE.updateRequestToObj(updateRequest);
        knowledgeBaseService.validKnowledgeBase(knowledgeBase, false);
        boolean result = knowledgeBaseService.updateById(knowledgeBase);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取知识库详情
     *
     * @param id      记录 ID
     * @param request 请求对象
     * @return 知识库详情
     */
    @GetMapping("/get/vo")
    @Operation(summary = "根据 id 获取知识库详情")
    public BaseResponse<KnowledgeBaseVO> getKnowledgeBaseVOById(Long id, HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        KnowledgeBase knowledgeBase = knowledgeBaseService.getById(id);
        ThrowUtils.throwIf(knowledgeBase == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(knowledgeBaseService.getKnowledgeBaseVO(knowledgeBase, request));
    }

    /**
     * 分页获取我的知识库列表
     *
     * @param queryRequest 分页查询请求
     * @param request      请求对象
     * @return 知识库列表分页
     */
    @PostMapping("/my/list/page/vo")
    @Operation(summary = "分页获取我的知识库列表")
    public BaseResponse<Page<KnowledgeBaseVO>> listMyKnowledgeBaseVOByPage(
            @RequestBody KnowledgeBaseQueryRequest queryRequest, HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        // 限制查询页大小
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);
        queryRequest.setUserId(SecurityUtils.getLoginUserId());
        Page<KnowledgeBase> knowledgeBasePage = knowledgeBaseService.page(new Page<>(current, size),
                knowledgeBaseService.getQueryWrapper(queryRequest));
        return ResultUtils.success(knowledgeBaseService.getKnowledgeBaseVOPage(knowledgeBasePage, request));
    }

    /**
     * 分页获取知识库列表 (管理员)
     *
     * @param queryRequest 分页查询请求
     * @param request      请求对象
     * @return 知识库列表分页
     */
    @PostMapping("/list/page/vo")
    @Operation(summary = "管理员分页获取知识库列表")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<KnowledgeBaseVO>> listKnowledgeBaseVOByPage(
            @RequestBody KnowledgeBaseQueryRequest queryRequest, HttpServletRequest request) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);
        Page<KnowledgeBase> knowledgeBasePage = knowledgeBaseService.page(new Page<>(current, size),
                knowledgeBaseService.getQueryWrapper(queryRequest));
        return ResultUtils.success(knowledgeBaseService.getKnowledgeBaseVOPage(knowledgeBasePage, request));
    }

    /**
     * 知识库检索
     *
     * @param knowledgeRetrievalRequest 检索请求
     * @return 匹配的切片列表
     */
    @PostMapping("/search")
    @Operation(summary = "检索知识库内容", description = "获取特定查询在知识库中的相似切片和评分")
    @OperationLog(module = "知识库模块", action = "检索知识库内容")
    public BaseResponse<List<ChunkSourceVO>> search(@RequestBody KnowledgeRetrievalRequest knowledgeRetrievalRequest) {
        Long loginUserId = SecurityUtils.getLoginUserId();
        List<ChunkSourceVO> result = knowledgeRetrievalService.search(knowledgeRetrievalRequest, loginUserId);
        return ResultUtils.success(result);
    }

}
