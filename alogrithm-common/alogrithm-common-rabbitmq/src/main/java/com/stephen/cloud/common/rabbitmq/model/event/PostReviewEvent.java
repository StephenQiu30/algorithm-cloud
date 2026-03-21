package com.stephen.cloud.common.rabbitmq.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 帖子审核事件
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostReviewEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 帖子 ID
     */
    private Long postId;

    /**
     * 作者 ID
     */
    private Long authorId;

    /**
     * 帖子标题
     */
    private String postTitle;

    /**
     * 审核状态
     */
    private Integer status;

    /**
     * 审核信息
     */
    private String message;
}
