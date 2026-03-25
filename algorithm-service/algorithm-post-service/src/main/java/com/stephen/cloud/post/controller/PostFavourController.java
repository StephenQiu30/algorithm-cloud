package com.stephen.cloud.post.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.post.model.dto.favour.PostFavourQueryRequest;
import com.stephen.cloud.api.post.model.dto.favour.PostFavourRequest;
import com.stephen.cloud.api.post.model.dto.post.PostQueryRequest;
import com.stephen.cloud.api.post.model.vo.PostVO;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.post.model.entity.Post;
import com.stephen.cloud.post.service.PostFavourService;
import com.stephen.cloud.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 帖子收藏管理接口
 * <p>
 * 提供帖子收藏/取消收藏、收藏列表查询等功能。
 * 收藏操作影响帖子的 favourNum 字段，支持异步更新。
 * </p>
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/post/favour")
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "PostFavourController", description = "帖子收藏管理")
public class PostFavourController {

    @Resource
    private PostFavourService postFavourService;

    @Resource
    private PostService postService;

    /**
     * 收藏/取消收藏帖子
     *
     * @param postFavourRequest 收藏请求
     * @return 收藏状态（0-取消收藏，1-收藏成功）
     */
    @PostMapping("/add")
    @Operation(summary = "收藏/取消收藏", description = "收藏或取消收藏指定帖子")
    public BaseResponse<Integer> doFavour(@RequestBody PostFavourRequest postFavourRequest) {
        if (postFavourRequest == null || postFavourRequest.getPostId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Long userId = StpUtil.getLoginIdAsLong();
        long postId = postFavourRequest.getPostId();
        int result = postFavourService.doPostFavour(postId, userId);
        return ResultUtils.success(result);
    }

    /**
     * 获取我收藏的帖子列表
     *
     * @param postQueryRequest postQueryRequest
     * @param request          request
     * @return BaseResponse<Page<PostVO>>
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<PostVO>> listMyFavourPostByPage(@RequestBody PostQueryRequest postQueryRequest,
                                                             HttpServletRequest request) {
        if (postQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Long userId = StpUtil.getLoginIdAsLong();
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Post> postPage = postFavourService.listFavourPostByPage(new Page<>(current, size),
                postService.getQueryWrapper(postQueryRequest), userId);
        return ResultUtils.success(postService.getPostVOPage(postPage, request));
    }

    /**
     * 获取用户收藏的帖子列表
     *
     * @param postFavourQueryRequest postFavourQueryRequest
     * @param request                request
     * @return BaseResponse<Page<PostVO>>
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<PostVO>> listFavourPostByPage(@RequestBody PostFavourQueryRequest postFavourQueryRequest,
                                                           HttpServletRequest request) {
        if (postFavourQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = postFavourQueryRequest.getCurrent();
        long size = postFavourQueryRequest.getPageSize();
        Long userIdLong = postFavourQueryRequest.getUserId();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20 || userIdLong == null, ErrorCode.PARAMS_ERROR);
        long userId = userIdLong.longValue();
        Page<Post> postPage = postFavourService.listFavourPostByPage(new Page<>(current, size),
                postService.getQueryWrapper(postFavourQueryRequest.getPostQueryRequest()), userId);
        return ResultUtils.success(postService.getPostVOPage(postPage, request));
    }
}
