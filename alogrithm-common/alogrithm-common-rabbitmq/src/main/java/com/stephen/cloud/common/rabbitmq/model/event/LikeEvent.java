package com.stephen.cloud.common.rabbitmq.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 点赞事件模型
 * <p>
 * 用于在点赞时触发通知
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 点赞 ID
     */
    private Long likeId;

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
     * 点赞用户 ID
     */
    private Long likeUserId;

    /**
     * 点赞用户昵称
     */
    private String likeUserName;
}
