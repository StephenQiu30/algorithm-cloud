package com.stephen.cloud.common.rabbitmq.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 评论事件模型
 * <p>
 * 用于在评论创建时触发通知
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论 ID
     */
    private Long commentId;

    /**
     * 帖子 ID
     */
    private Long postId;

    /**
     * 帖子标题
     */
    private String postTitle;

    /**
     * 帖子作者 ID（接收通知的用户）
     */
    private Long postAuthorId;

    /**
     * 评论作者 ID
     */
    private Long commentAuthorId;

    /**
     * 评论作者昵称
     */
    private String commentAuthorName;

    /**
     * 评论内容
     */
    private String commentContent;
}
