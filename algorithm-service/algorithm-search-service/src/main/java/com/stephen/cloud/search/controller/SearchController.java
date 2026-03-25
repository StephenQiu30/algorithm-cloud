package com.stephen.cloud.search.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.post.model.dto.post.PostQueryRequest;
import com.stephen.cloud.api.search.model.dto.SearchRequest;
import com.stephen.cloud.api.search.model.vo.SearchVO;
import com.stephen.cloud.api.search.model.entity.PostEsDTO;
import com.stephen.cloud.api.search.model.entity.UserEsDTO;
import com.stephen.cloud.api.user.model.dto.UserQueryRequest;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.log.annotation.OperationLog;
import com.stephen.cloud.search.manager.EsIndexManager;
import com.stephen.cloud.search.manager.SearchFacade;
import com.stephen.cloud.search.service.PostEsService;
import com.stephen.cloud.search.service.UserEsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 搜索服务接口
 * <p>
 * 提供基于 Elasticsearch 的全文搜索能力，支持帖子、用户等多类型数据的聚合搜索。
 * 支持从 ES 分页查询、批量同步等功能，内部通过 {@link com.stephen.cloud.search.manager.SearchFacade} 统一调度。
 * </p>
 *
 * @author stephen
 */
@RestController
@RequestMapping("/search")
@Slf4j
@Tag(name = "SearchController", description = "搜索服务")
public class SearchController {

    @Resource
    private SearchFacade searchFacade;

    @Resource
    private PostEsService postEsService;

    @Resource
    private UserEsService userEsService;

    @Resource
    private EsIndexManager esIndexManager;

    /**
     * 分页搜索帖子（从 ES 查询）
     *
     * @param postQueryRequest 查询请求
     * @param request          HTTP 请求
     * @return 分页结果
     */
    @Operation(summary = "分页搜索帖子（从 ES 查询）")
    @PostMapping("/post/page")
    @OperationLog(module = "搜索服务", action = "搜索帖子")
    public BaseResponse<Page<?>> searchPostByPage(@RequestBody PostQueryRequest postQueryRequest,
                                                  HttpServletRequest request) {
        long size = postQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<PostEsDTO> postPage = (Page<PostEsDTO>) postEsService.searchFromEs(postQueryRequest);
        return ResultUtils.success(postPage);
    }

    /**
     * 分页搜索用户（从 ES 查询）
     *
     * @param userQueryRequest 查询请求
     * @param request          HTTP 请求
     * @return 分页结果
     */
    @Operation(summary = "分页搜索用户（从 ES 查询）")
    @PostMapping("/user/page")
    @OperationLog(module = "搜索服务", action = "搜索用户")
    public BaseResponse<Page<?>> searchUserByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                  HttpServletRequest request) {
        long size = userQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<UserEsDTO> userPage = (Page<UserEsDTO>) userEsService.searchFromEs(userQueryRequest);
        return ResultUtils.success(userPage);
    }

    /**
     * 聚合搜索查询
     *
     * @param searchRequest 搜索请求
     * @param request       HTTP 请求
     * @return 搜索结果
     */
    @Operation(summary = "聚合搜索查询")
    @PostMapping("/all")
    @OperationLog(module = "搜索服务", action = "聚合搜索")
    public BaseResponse<SearchVO<Object>> doSearchAll(@RequestBody SearchRequest searchRequest,
                                                      HttpServletRequest request) {
        return ResultUtils.success(searchFacade.searchAll(searchRequest, request));
    }

    /**
     * 批量同步帖子到 ES
     *
     * @param postEsDTOList 帖子列表
     * @return 是否成功
     */
    @Operation(summary = "批量同步帖子到 ES")
    @PostMapping("/post/batch/upsert")
    @OperationLog(module = "搜索服务", action = "批量同步帖子")
    public BaseResponse<Boolean> batchUpsertPost(@RequestBody List<PostEsDTO> postEsDTOList) {
        return ResultUtils.success(postEsService.batchUpsert(postEsDTOList));
    }

    /**
     * 批量同步用户到 ES
     *
     * @param userEsDTOList 用户列表
     * @return 是否成功
     */
    @Operation(summary = "批量同步用户到 ES")
    @PostMapping("/user/batch/upsert")
    @OperationLog(module = "搜索服务", action = "批量同步用户")
    public BaseResponse<Boolean> batchUpsertUser(@RequestBody List<UserEsDTO> userEsDTOList) {
        return ResultUtils.success(userEsService.batchUpsert(userEsDTOList));
    }

}
