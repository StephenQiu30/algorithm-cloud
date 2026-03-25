package com.stephen.cloud.post.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.post.model.dto.comment.PostCommentAddRequest;
import com.stephen.cloud.api.post.model.dto.comment.PostCommentEditRequest;
import com.stephen.cloud.api.post.model.dto.comment.PostCommentQueryRequest;
import com.stephen.cloud.api.post.model.dto.comment.PostCommentUpdateRequest;
import com.stephen.cloud.api.post.model.vo.PostCommentVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.common.*;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.log.annotation.OperationLog;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.event.CommentEvent;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import com.stephen.cloud.post.convert.PostCommentConvert;
import com.stephen.cloud.post.model.entity.Post;
import com.stephen.cloud.post.model.entity.PostComment;
import com.stephen.cloud.post.service.PostCommentService;
import com.stephen.cloud.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 帖子评论管理接口
 * <p>
 * 提供帖子的评论 CRUD、分页查询、我的评论列表等功能。
 * 评论创建后支持异步发送通知事件，详见 {@link com.stephen.cloud.common.rabbitmq.model.event.CommentEvent}
 * </p>
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/post/comment")
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "PostCommentController", description = "帖子评论管理")
public class PostCommentController {

    @Resource
    private PostCommentService postCommentService;

    @Resource
    private PostService postService;

    @Resource
    private RabbitMqSender mqSender;

    @Resource
    private UserFeignClient userFeignClient;

    /**
     * 创建帖子评论
     *
     * @param postCommentAddRequest postCommentAddRequest
     * @return {@link BaseResponse<Long>}
     */
    @PostMapping("/add")
    @OperationLog(module = "评论管理", action = "创建评论")
    @Operation(summary = "创建帖子评论", description = "创建新的帖子评论")
    public BaseResponse<Long> addPostComment(@RequestBody PostCommentAddRequest postCommentAddRequest) {
        PostComment postComment = PostCommentConvert.addRequestToObj(postCommentAddRequest);
        // 数据校验
        postCommentService.validPostComment(postComment, true);

        // 填充默认值
        Long loginUserId = StpUtil.getLoginIdAsLong();
        postComment.setUserId(loginUserId);
        boolean result = postCommentService.save(postComment);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        try {
            Post post = postService.getById(postComment.getPostId());
            if (post != null && post.getUserId() != null) {
                UserVO userVO = userFeignClient.getUserVOById(loginUserId).getData();
                CommentEvent commentEvent = CommentEvent.builder()
                        .commentId(postComment.getId())
                        .postId(postComment.getPostId())
                        .postTitle(post.getTitle())
                        .postAuthorId(post.getUserId())
                        .commentAuthorId(loginUserId)
                        .commentAuthorName(userVO == null ? null : userVO.getUserName())
                        .commentContent(postComment.getContent())
                        .build();
                mqSender.sendTransactional(MqBizTypeEnum.COMMENT_EVENT, "comment:" + postComment.getId(), commentEvent);
            }
        } catch (Exception e) {
            log.error("[PostCommentController] 发送评论通知失败, commentId: {}", postComment.getId(), e);
        }

        // 返回新写入的数据 id
        long newPostCommentId = postComment.getId();
        return ResultUtils.success(newPostCommentId);
    }

    /**
     * 删除帖子评论
     *
     * @param deleteRequest deleteRequest
     * @return {@link BaseResponse<Boolean>}
     */
    @PostMapping("/delete")
    @OperationLog(module = "评论管理", action = "删除评论")
    @Operation(summary = "删除帖子评论", description = "删除指定帖子评论，仅本人或管理员可操作")
    public BaseResponse<Boolean> deletePostComment(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long loginUserId = StpUtil.getLoginIdAsLong();
        long id = deleteRequest.getId();
        // 判断是否存在
        PostComment oldPostComment = postCommentService.getById(id);
        if (oldPostComment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldPostComment.getUserId().equals(loginUserId)
                && !StpUtil.hasRole(UserConstant.ADMIN_ROLE)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = postCommentService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新帖子评论（仅管理员可用）
     *
     * @param postCommentUpdateRequest postCommentUpdateRequest
     * @return {@link BaseResponse<Boolean>}
     */
    @PostMapping("/update")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "更新帖子评论（管理员）", description = "更新帖子评论信息（仅管理员可用）")
    public BaseResponse<Boolean> updatePostComment(
            @RequestBody PostCommentUpdateRequest postCommentUpdateRequest) {
        PostComment postComment = PostCommentConvert.updateRequestToObj(postCommentUpdateRequest);
        // 数据校验
        postCommentService.validPostComment(postComment, false);
        // 判断是否存在
        long id = postCommentUpdateRequest.getId();
        PostComment oldPostComment = postCommentService.getById(id);
        ThrowUtils.throwIf(oldPostComment == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = postCommentService.updateById(postComment);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取帖子评论（封装类）
     *
     * @param id id
     * @return {@link BaseResponse<PostCommentVO>}
     */
    @GetMapping("/get/vo")
    @Operation(summary = "获取帖子评论详情", description = "根据 ID 获取帖子评论详细信息")
    public BaseResponse<PostCommentVO> getPostCommentVOById(@RequestParam("id") long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        PostComment postComment = postCommentService.getById(id);
        ThrowUtils.throwIf(postComment == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(postCommentService.getPostCommentVO(postComment, request));
    }

    /**
     * 分页获取帖子评论列表（仅管理员可用）
     *
     * @param postCommentQueryRequest postCommentQueryRequest
     * @return {@link BaseResponse<Page<PostComment>>}
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "分页获取帖子评论列表（管理员）", description = "分页获取帖子评论列表（仅管理员可用）")
    public BaseResponse<Page<PostCommentVO>> listPostCommentByPage(
            @RequestBody PostCommentQueryRequest postCommentQueryRequest, HttpServletRequest request) {
        long current = postCommentQueryRequest.getCurrent();
        long size = postCommentQueryRequest.getPageSize();
        // 查询数据库
        Page<PostComment> postCommentPage = postCommentService.page(new Page<>(current, size),
                postCommentService.getQueryWrapper(postCommentQueryRequest));
        return ResultUtils.success(postCommentService.getPostCommentVOPage(postCommentPage, request));
    }

    /**
     * 分页获取帖子评论列表（封装类）
     *
     * @param postCommentQueryRequest postCommentQueryRequest
     * @return {@link BaseResponse<Page<PostCommentVO>>}
     */
    @PostMapping("/list/page/vo")
    @Operation(summary = "分页获取帖子评论列表", description = "分页获取帖子评论详细信息列表")
    public BaseResponse<Page<PostCommentVO>> listPostCommentVOByPage(
            @RequestBody PostCommentQueryRequest postCommentQueryRequest, HttpServletRequest request) {
        long current = postCommentQueryRequest.getCurrent();
        long size = postCommentQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<PostComment> postCommentPage = postCommentService.page(new Page<>(current, size),
                postCommentService.getQueryWrapper(postCommentQueryRequest));
        // 获取封装类
        return ResultUtils.success(postCommentService.getPostCommentVOPage(postCommentPage, request));
    }

    /**
     * 分页获取当前登录用户创建的帖子评论列表
     *
     * @param postCommentQueryRequest postCommentQueryRequest
     * @return {@link BaseResponse<Page<PostCommentVO>>}
     */
    @PostMapping("/my/list/page/vo")
    @Operation(summary = "我的帖子评论列表", description = "分页获取当前登录用户创建的帖子评论列表")
    public BaseResponse<Page<PostCommentVO>> listMyPostCommentVOByPage(
            @RequestBody PostCommentQueryRequest postCommentQueryRequest, HttpServletRequest request) {
        if (postCommentQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 补充查询条件，只查询当前登录用户的数据
        Long loginUserId = StpUtil.getLoginIdAsLong();
        postCommentQueryRequest.setUserId(loginUserId);
        long current = postCommentQueryRequest.getCurrent();
        long size = postCommentQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<PostComment> postCommentPage = postCommentService.page(new Page<>(current, size),
                postCommentService.getQueryWrapper(postCommentQueryRequest));
        // 获取封装类
        return ResultUtils.success(postCommentService.getPostCommentVOPage(postCommentPage, request));
    }

    /**
     * 编辑帖子评论（给用户使用）
     *
     * @param postCommentEditRequest postCommentEditRequest
     * @return {@link BaseResponse<Boolean>}
     */
    @PostMapping("/edit")
    @Operation(summary = "编辑帖子评论", description = "编辑帖子评论信息，仅本人或管理员可操作")
    public BaseResponse<Boolean> editPostComment(
            @RequestBody PostCommentEditRequest postCommentEditRequest) {
        PostComment postComment = PostCommentConvert.editRequestToObj(postCommentEditRequest);
        postCommentService.validPostComment(postComment, false);
        Long loginUserId = StpUtil.getLoginIdAsLong();
        long id = postCommentEditRequest.getId();
        PostComment oldPostComment = postCommentService.getById(id);
        if (oldPostComment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可编辑
        if (!oldPostComment.getUserId().equals(loginUserId)
                && !StpUtil.hasRole(UserConstant.ADMIN_ROLE)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = postCommentService.updateById(postComment);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

}
