package com.stephen.cloud.post.convert;

import com.stephen.cloud.api.post.model.dto.comment.PostCommentAddRequest;
import com.stephen.cloud.api.post.model.dto.comment.PostCommentEditRequest;
import com.stephen.cloud.api.post.model.dto.comment.PostCommentUpdateRequest;
import com.stephen.cloud.api.post.model.vo.PostCommentVO;
import com.stephen.cloud.post.model.entity.PostComment;
import org.springframework.beans.BeanUtils;

/**
 * 帖子评论转换器
 *
 * @author StephenQiu30
 */
public class PostCommentConvert {

    /**
     * 对象转视图
     *
     * @param postComment 帖子评论实体
     * @return 帖子评论视图
     */
    public static PostCommentVO objToVo(PostComment postComment) {
        if (postComment == null) {
            return null;
        }
        PostCommentVO postCommentVO = new PostCommentVO();
        BeanUtils.copyProperties(postComment, postCommentVO);
        return postCommentVO;
    }

    /**
     * 新增请求转对象
     *
     * @param postCommentAddRequest 新增请求
     * @return 帖子评论实体
     */
    public static PostComment addRequestToObj(PostCommentAddRequest postCommentAddRequest) {
        if (postCommentAddRequest == null) {
            return null;
        }
        PostComment postComment = new PostComment();
        BeanUtils.copyProperties(postCommentAddRequest, postComment);
        return postComment;
    }

    /**
     * 更新请求转对象
     *
     * @param postCommentUpdateRequest 更新请求
     * @return 帖子评论实体
     */
    public static PostComment updateRequestToObj(PostCommentUpdateRequest postCommentUpdateRequest) {
        if (postCommentUpdateRequest == null) {
            return null;
        }
        PostComment postComment = new PostComment();
        BeanUtils.copyProperties(postCommentUpdateRequest, postComment);
        return postComment;
    }

    /**
     * 编辑请求转对象
     *
     * @param postCommentEditRequest 编辑请求
     * @return 帖子评论实体
     */
    public static PostComment editRequestToObj(PostCommentEditRequest postCommentEditRequest) {
        if (postCommentEditRequest == null) {
            return null;
        }
        PostComment postComment = new PostComment();
        BeanUtils.copyProperties(postCommentEditRequest, postComment);
        return postComment;
    }

    /**
     * 视图转对象
     *
     * @param postCommentVO 帖子评论视图
     * @return 帖子评论实体
     */
    public static PostComment voToObj(PostCommentVO postCommentVO) {
        if (postCommentVO == null) {
            return null;
        }
        PostComment postComment = new PostComment();
        BeanUtils.copyProperties(postCommentVO, postComment);
        return postComment;
    }
}
