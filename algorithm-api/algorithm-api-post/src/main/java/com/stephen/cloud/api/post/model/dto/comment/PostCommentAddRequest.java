package com.stephen.cloud.api.post.model.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建评论请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "创建评论请求")
public class PostCommentAddRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论内容
     */
    @Schema(description = "评论内容")
    private String content;

    /**
     * 帖子ID
     */
    @Schema(description = "帖子ID")
    private Long postId;

    /**
     * 父评论ID
     */
    @Schema(description = "父评论ID")
    private Long parentId;
}
