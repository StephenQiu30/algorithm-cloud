package com.stephen.cloud.ai.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.convert.KnowledgeBaseConvert;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.ai.service.KnowledgeBaseService;
import com.stephen.cloud.api.knowledge.model.dto.retrieval.KnowledgeRetrievalRequest;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import com.stephen.cloud.api.knowledge.model.dto.knowledgebase.KnowledgeBaseAddRequest;
import com.stephen.cloud.api.knowledge.model.dto.knowledgebase.KnowledgeBaseQueryRequest;
import com.stephen.cloud.api.knowledge.model.dto.knowledgebase.KnowledgeBaseUpdateRequest;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeBaseVO;
import java.util.List;
import java.util.Map;
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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库管理接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/ai/knowledge")
@Slf4j
@Tag(name = "KnowledgeBaseController", description = "知识库管理接口")
public class KnowledgeBaseController {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 创建知识库
     *
     * @param addRequest 创建请求
     * @return 记录 ID
     */
    @PostMapping("/add")
    @Operation(summary = "创建知识库", description = "新建一个知识库容器，用于存放相关的教学文档。")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "知识库模块", action = "创建知识库")
    public BaseResponse<Long> addKnowledgeBase(@RequestBody KnowledgeBaseAddRequest addRequest) {
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
     * @return 是否成功
     */
    @PostMapping("/delete")
    @Operation(summary = "删除知识库", description = "删除指定的知识库信息（通常为管理员操作）。")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "知识库模块", action = "删除知识库")
    public BaseResponse<Boolean> deleteKnowledgeBase(@RequestBody DeleteRequest deleteRequest) {
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
     * @return 是否成功
     */
    @PostMapping("/update")
    @Operation(summary = "管理员更新知识库", description = "系统管理员全量更新指定的知识库详情。")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "知识库模块", action = "管理员更新知识库")
    public BaseResponse<Boolean> updateKnowledgeBase(@RequestBody KnowledgeBaseUpdateRequest updateRequest) {
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
     * 根据 ID 获取知识库详情
     *
     * @param id      记录 ID
     * @param request 请求对象
     * @return 知识库视图对象
     */
    @GetMapping("/get/vo")
    @Operation(summary = "获取知识库详情", description = "根据主键 ID 查询知识库的详细信息。")
    public BaseResponse<KnowledgeBaseVO> getKnowledgeBaseVOById(@Parameter(description = "知识库 ID") @RequestParam("id") Long id, HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        KnowledgeBase knowledgeBase = knowledgeBaseService.getById(id);
        ThrowUtils.throwIf(knowledgeBase == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(knowledgeBaseService.getKnowledgeBaseVO(knowledgeBase, request));
    }

    /**
     * 分页获取当前登录用户的知识库列表
     *
     * @param queryRequest 分页查询请求
     * @param request      请求对象
     * @return 知识库视图分页
     */
    @PostMapping("/my/list/page/vo")
    @Operation(summary = "分页获取我的知识库列表", description = "获取当前登录用户创建的所有知识库列表。")
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
     * 分页获取所有知识库列表 (管理员)
     *
     * @param queryRequest 分页查询请求
     * @param request      请求对象
     * @return 知识库视图分页
     */
    @PostMapping("/list/page/vo")
    @Operation(summary = "管理员分页获取知识库列表", description = "管理员视角分页检索系统内所有知识库。")
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
     * 知识库语义检索
     *
     * @param knowledgeRetrievalRequest 检索参数
     * @return 关联的分片来源列表
     */
    @PostMapping("/search")
    @Operation(summary = "检索知识库内容", description = "基于向量相似度的语义搜索，返回匹配的文档片段。")
    @OperationLog(module = "知识库模块", action = "检索知识库内容")
    public BaseResponse<List<ChunkSourceVO>> search(@RequestBody KnowledgeRetrievalRequest knowledgeRetrievalRequest) {
        Long loginUserId = SecurityUtils.getLoginUserId();
        List<ChunkSourceVO> result = knowledgeBaseService.searchChunks(knowledgeRetrievalRequest, loginUserId);
        return ResultUtils.success(result);
    }

    /**
     * 诊断双路召回（调试用）
     *
     * @param knowledgeRetrievalRequest 检索参数
     * @return kNN、BM25、Hybrid 三路结果
     */
    @PostMapping("/search/diagnose")
    @Operation(summary = "诊断双路召回", description = "返回 kNN、BM25 和 RRF 融合三路结果，用于调参优化。")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "知识库模块", action = "诊断双路召回")
    public BaseResponse<Map<String, List<ChunkSourceVO>>> diagnoseSearch(@RequestBody KnowledgeRetrievalRequest knowledgeRetrievalRequest) {
        Long loginUserId = SecurityUtils.getLoginUserId();
        Map<String, List<ChunkSourceVO>> result = knowledgeBaseService.diagnoseHybridSearch(knowledgeRetrievalRequest, loginUserId);
        return ResultUtils.success(result);
    }
}
