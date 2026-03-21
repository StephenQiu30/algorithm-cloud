package com.stephen.cloud.post.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.post.model.dto.comment.PostCommentQueryRequest;
import com.stephen.cloud.api.post.model.vo.PostCommentVO;
import com.stephen.cloud.post.model.entity.PostComment;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 帖子评论服务
 *
 * @author StephenQiu30
 */
public interface PostCommentService extends IService<PostComment> {

    /**
     * 校验评论信息
     *
     * @param postComment postComment
     * @param add         add 是否是添加
     */
    void validPostComment(PostComment postComment, boolean add);

    /**
     * 获取查询包装类
     *
     * @param postCommentQueryRequest postCommentQueryRequest
     * @return {@link LambdaQueryWrapper<PostComment>}
     */
    LambdaQueryWrapper<PostComment> getQueryWrapper(PostCommentQueryRequest postCommentQueryRequest);

    /**
     * 获取评论视图类
     *
     * @param postComment postComment
     * @param request     request
     * @return {@link PostCommentVO}
     */
    PostCommentVO getPostCommentVO(PostComment postComment, HttpServletRequest request);

    /**
     * 分页获取评论视图类
     *
     * @param postCommentPage postCommentPage
     * @param request         request
     * @return {@link Page<PostCommentVO>}
     */
    Page<PostCommentVO> getPostCommentVOPage(Page<PostComment> postCommentPage, HttpServletRequest request);
}
