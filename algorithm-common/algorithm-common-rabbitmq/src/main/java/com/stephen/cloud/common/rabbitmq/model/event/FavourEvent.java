package com.stephen.cloud.common.rabbitmq.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 收藏事件
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavourEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 收藏记录 ID
     */
    private Long favourId;

    /**
     * 帖子 ID
     */
    private Long postId;

    /**
     * 帖子作者 ID
     */
    private Long postAuthorId;

    /**
     * 帖子标题
     */
    private String postTitle;

    /**
     * 收藏者 ID
     */
    private Long favourUserId;

    /**
     * 收藏者昵称
     */
    private String favourUserName;
}
