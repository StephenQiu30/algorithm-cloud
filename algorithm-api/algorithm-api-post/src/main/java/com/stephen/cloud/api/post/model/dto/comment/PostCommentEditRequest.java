package com.stephen.cloud.api.post.model.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 编辑帖子评论请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "编辑帖子评论请求")
public class PostCommentEditRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     * 必填，指定要编辑的评论
     */
    @Schema(description = "评论ID")
    private Long id;

    /**
     * 评论内容
     * 必填，新的评论内容
     */
    @Schema(description = "评论内容")
    private String content;
}
