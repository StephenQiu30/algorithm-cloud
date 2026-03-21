package com.stephen.cloud.api.post.model.dto.thumb;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 帖子点赞请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "帖子点赞请求")
public class PostThumbRequest implements Serializable {

    /**
     * 帖子ID
     * 必填，指定要点赞/取消点赞的帖子
     */
    @Schema(description = "帖子ID")
    private Long postId;

    @Serial
    private static final long serialVersionUID = 1L;
}
