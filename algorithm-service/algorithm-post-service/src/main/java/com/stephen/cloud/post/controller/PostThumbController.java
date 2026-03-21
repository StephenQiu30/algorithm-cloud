package com.stephen.cloud.post.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.post.model.dto.favour.PostFavourQueryRequest;
import com.stephen.cloud.api.post.model.dto.post.PostQueryRequest;
import com.stephen.cloud.api.post.model.dto.thumb.PostThumbRequest;
import com.stephen.cloud.api.post.model.vo.PostVO;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.post.model.entity.Post;
import com.stephen.cloud.post.service.PostService;
import com.stephen.cloud.post.service.PostThumbService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 帖子点赞接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/post/thumb")
@Slf4j
public class PostThumbController {

    @Resource
    private PostThumbService postThumbService;

    @Resource
    private PostService postService;

    /**
     * 点赞/取消点赞帖子
     *
     * @param postThumbRequest 点赞请求
     * @return 点赞状态（0-取消点赞，1-点赞成功）
     */
    @PostMapping("/add")
    @Operation(summary = "点赞/取消点赞", description = "点赞或取消点赞指定帖子")
    public BaseResponse<Integer> doThumb(@RequestBody PostThumbRequest postThumbRequest) {
        if (postThumbRequest == null || postThumbRequest.getPostId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Long userId = StpUtil.getLoginIdAsLong();
        long postId = postThumbRequest.getPostId();
        int result = postThumbService.doPostThumb(postId, userId);
        return ResultUtils.success(result);
    }

    /**
     * 获取我点赞的帖子列表
     *
     * @param postQueryRequest postQueryRequest
     * @param request          request
     * @return BaseResponse<Page<PostVO>>
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<PostVO>> listMyThumbPostByPage(@RequestBody PostQueryRequest postQueryRequest,
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
        Page<Post> postPage = postThumbService.listThumbPostByPage(new Page<>(current, size),
                postService.getQueryWrapper(postQueryRequest), userId);
        return ResultUtils.success(postService.getPostVOPage(postPage, request));
    }

    /**
     * 获取用户点赞的帖子列表
     *
     * @param postFavourQueryRequest postFavourQueryRequest
     * @param request                request
     * @return BaseResponse<Page<PostVO>>
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<PostVO>> listThumbPostByPage(@RequestBody PostFavourQueryRequest postFavourQueryRequest,
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
        Page<Post> postPage = postThumbService.listThumbPostByPage(new Page<>(current, size),
                postService.getQueryWrapper(postFavourQueryRequest.getPostQueryRequest()), userId);
        return ResultUtils.success(postService.getPostVOPage(postPage, request));
    }
}
