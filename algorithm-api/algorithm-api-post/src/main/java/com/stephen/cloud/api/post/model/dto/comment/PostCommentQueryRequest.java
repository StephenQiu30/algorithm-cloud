package com.stephen.cloud.api.post.model.dto.comment;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询评论请求
 *
 * @author StephenQiu30
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "查询评论请求")
public class PostCommentQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @Schema(description = "评论ID")
    private Long id;

    /**
     * 帖子ID
     */
    @Schema(description = "帖子ID")
    private Long postId;

    /**
     * 评论用户ID
     */
    @Schema(description = "评论用户ID")
    private Long userId;

    /**
     * 父评论ID
     */
    @Schema(description = "父评论ID")
    private Long parentId;

    /**
     * 评论内容
     */
    @Schema(description = "评论内容")
    private String content;
}
