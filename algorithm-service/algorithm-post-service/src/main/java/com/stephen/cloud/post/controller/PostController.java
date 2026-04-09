package com.stephen.cloud.post.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.post.model.dto.post.PostAddRequest;
import com.stephen.cloud.api.post.model.dto.post.PostEditRequest;
import com.stephen.cloud.api.post.model.dto.post.PostQueryRequest;
import com.stephen.cloud.api.post.model.dto.post.PostUpdateRequest;
import com.stephen.cloud.api.post.model.dto.review.PostReviewRequest;
import com.stephen.cloud.api.post.model.enums.PostReviewStatusEnum;
import com.stephen.cloud.api.post.model.vo.PostVO;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.*;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.log.annotation.OperationLog;
import com.stephen.cloud.post.convert.PostConvert;
import com.stephen.cloud.post.model.entity.Post;
import com.stephen.cloud.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 帖子管理接口
 * <p>
 * 提供帖子的增删改查、审核、分页查询及 ES 同步等功能。
 * 所有需要登录态的接口通过 Sa-Token 进行身份认证。
 * 帖子创建/编辑后自动进入待审核状态，需管理员复审后方可公开可见。
 * </p>
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/post")
@Slf4j
@Tag(name = "PostController", description = "帖子管理")
public class PostController {

    @Resource
    private PostService postService;

    /**
     * 创建帖子
     * <p>
     * 创建新帖子，默认状态为待审核。
     *
     * @param postAddRequest 帖子创建请求
     * @param request        HTTP 请求
     * @return 新创建的帖子 ID
     */
    @PostMapping("/add")
    @OperationLog(module = "帖子管理", action = "创建帖子")
    @Operation(summary = "创建帖子", description = "创建新帖子，初始状态为待审核")
    public BaseResponse<Long> addPost(@RequestBody PostAddRequest postAddRequest,
                                      HttpServletRequest request) {
        Post post = PostConvert.addRequestToObj(postAddRequest);
        postService.validPost(post, true);

        // 初始帖子状态设为待审核，需经过人工审核或自动过滤后才对公众可见
        post.setReviewStatus(PostReviewStatusEnum.REVIEWING.getValue());
        post.setReviewMessage("等待人工复审");

        Long userId = SecurityUtils.getLoginUserId();
        post.setUserId(userId);
        post.setFavourNum(0);
        post.setThumbNum(0);
        boolean result = postService.save(post);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 同步 ES 索引
        if (result) {
            postService.syncToEs(post.getId());
        }

        return ResultUtils.success(post.getId());
    }

    /**
     * 删除帖子
     *
     * @param deleteRequest 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @OperationLog(module = "帖子管理", action = "删除帖子")
    @Operation(summary = "删除帖子", description = "删除指定帖子，仅本人或管理员可操作")
    public BaseResponse<Boolean> deletePost(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = SecurityUtils.getLoginUserId();
        long id = deleteRequest.getId();
        // 判断是否存在
        Post oldPost = postService.getById(id);
        if (oldPost == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldPost.getUserId().equals(userId) && !SecurityUtils.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = postService.removeById(id);

        // 同步 ES 索引
        if (b) {
            postService.syncToEs(id);
        }

        return ResultUtils.success(b);
    }

    /**
     * 更新帖子（仅限管理员权限）
     * 允许全量更新帖子字段，不改变审核状态（除非显式传入）
     *
     * @param postUpdateRequest 帖子更新请求参数
     * @return 更新结果（true: 成功, false: 失败）
     */
    @PostMapping("/update")
    @OperationLog(module = "帖子管理", action = "更新帖子")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "管理员更新帖子", description = "系统管理员全量更新指定帖子信息")
    public BaseResponse<Boolean> updatePost(@RequestBody PostUpdateRequest postUpdateRequest) {
        Post post = PostConvert.updateRequestToObj(postUpdateRequest);
        // 参数校验
        postService.validPost(post, false);
        long id = postUpdateRequest.getId();
        // 判断是否存在
        Post oldPost = postService.getById(id);
        if (oldPost == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        boolean result = postService.updateById(post);

        // 同步 ES 索引
        if (result) {
            postService.syncToEs(id);
        }

        return ResultUtils.success(result);
    }

    /**
     * 根据ID获取帖子
     *
     * @param id 帖子ID
     * @return 帖子VO信息
     */
    @GetMapping("/get/vo")
    @Operation(summary = "获取帖子详情", description = "根据ID获取帖子详细信息")
    public BaseResponse<PostVO> getPostVOById(@RequestParam("id") long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Post post = postService.getById(id);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(postService.getPostVO(post, request));
    }

    /**
     * 分页获取帖子列表（用于同步）
     * <p>
     * 获取完整字段的帖子列表，主要用于 ES 或其他系统的同步。
     *
     * @param postQueryRequest 查询请求
     * @param request          HTTP 请求
     * @return 帖子 VO 分页列表
     */
    @PostMapping("/list/page")
    @Operation(summary = "分页获取帖子列表（用于同步）", description = "获取完整字段的帖子列表，主要用于数据同步")
    public BaseResponse<Page<PostVO>> listPostByPage(@RequestBody PostQueryRequest postQueryRequest,
                                                     HttpServletRequest request) {
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();
        Page<Post> postPage = postService.page(new Page<>(current, size),
                postService.getQueryWrapper(postQueryRequest));
        return ResultUtils.success(postService.getPostVOPage(postPage, request));
    }

    /**
     * 分页获取帖子列表（封装类）
     * <p>
     * 获取脱敏后的帖子列表，仅返回审核通过的帖子。
     *
     * @param postQueryRequest 查询请求
     * @param request          HTTP 请求
     * @return 帖子 VO 分页列表
     */
    @PostMapping("/list/page/vo")
    @Operation(summary = "分页获取帖子列表", description = "分页获取已审核通过的帖子脱敏信息列表")
    public BaseResponse<Page<PostVO>> listPostVOByPage(@RequestBody PostQueryRequest postQueryRequest,
                                                       HttpServletRequest request) {
        if (postQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 公开列表仅展示已审核通过的帖子，确保内容安全合规
        postQueryRequest.setReviewStatus(PostReviewStatusEnum.PASS.getValue());
        Page<Post> postPage = postService.page(new Page<>(current, size),
                postService.getQueryWrapper(postQueryRequest));
        return ResultUtils.success(postService.getPostVOPage(postPage, request));
    }

    /**
     * 我的帖子列表
     * <p>
     * 分页获取当前登录用户创建的帖子列表。
     *
     * @param postQueryRequest 查询请求
     * @param request          HTTP 请求
     * @return 用户的帖子 VO 分页列表
     */
    @PostMapping("/my/list/page/vo")
    @Operation(summary = "我的帖子列表", description = "分页获取当前登录用户创建的帖子列表")
    public BaseResponse<Page<PostVO>> listMyPostVOByPage(@RequestBody PostQueryRequest postQueryRequest,
                                                         HttpServletRequest request) {
        if (postQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = SecurityUtils.getLoginUserId();
        postQueryRequest.setUserId(userId);
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Post> postPage = postService.page(new Page<>(current, size),
                postService.getQueryWrapper(postQueryRequest));
        return ResultUtils.success(postService.getPostVOPage(postPage, request));
    }

    /**
     * 编辑帖子（用户）
     *
     * @param postEditRequest 帖子编辑请求
     * @return 是否编辑成功
     */
    @PostMapping("/edit")
    @OperationLog(module = "帖子管理", action = "编辑帖子")
    @Operation(summary = "编辑帖子", description = "编辑帖子信息，仅本人可操作")
    public BaseResponse<Boolean> editPost(@RequestBody PostEditRequest postEditRequest) {
        Post post = PostConvert.editRequestToObj(postEditRequest);
        // 参数校验
        postService.validPost(post, false);
        Long userId = SecurityUtils.getLoginUserId();
        long id = postEditRequest.getId();
        // 判断是否存在
        Post oldPost = postService.getById(id);
        if (oldPost == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可编辑
        ThrowUtils.throwIf(!oldPost.getUserId().equals(userId) && !StpUtil.hasRole(UserConstant.ADMIN_ROLE),
                ErrorCode.NO_AUTH_ERROR);

        // 编辑后重置为待审核状态
        post.setReviewStatus(PostReviewStatusEnum.REVIEWING.getValue());
        post.setReviewMessage("内容已更新，等待人工复审");

        boolean result = postService.updateById(post);

        // 同步 ES 索引
        if (result) {
            postService.syncToEs(id);
        }

        return ResultUtils.success(result);
    }

    /**
     * 审核帖子（仅管理员可用）
     *
     * @param postReviewRequest 审核请求
     * @return 是否审核成功
     */
    @PostMapping("/review")
    @OperationLog(module = "帖子管理", action = "审核帖子")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "审核帖子", description = "人工审核帖子（通过或拒绝）")
    public BaseResponse<Boolean> reviewPost(@RequestBody PostReviewRequest postReviewRequest) {
        boolean result = postService.doPostReview(postReviewRequest);
        return ResultUtils.success(result);
    }

}
