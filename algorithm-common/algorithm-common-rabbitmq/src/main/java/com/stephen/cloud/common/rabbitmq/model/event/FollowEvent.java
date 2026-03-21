package com.stephen.cloud.common.rabbitmq.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 关注事件模型
 * <p>
 * 用于在关注时触发通知
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关注记录 ID
     */
    private Long followId;

    /**
     * 被关注用户 ID（接收通知的用户）
     */
    private Long followedUserId;

    /**
     * 关注者 ID
     */
    private Long followerId;

    /**
     * 关注者昵称
     */
    private String followerName;
}
