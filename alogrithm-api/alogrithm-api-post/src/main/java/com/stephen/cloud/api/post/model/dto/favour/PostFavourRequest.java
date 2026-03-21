package com.stephen.cloud.api.post.model.dto.favour;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 帖子收藏请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "帖子收藏请求")
public class PostFavourRequest implements Serializable {

    /**
     * 帖子ID
     * 必填，指定要收藏/取消收藏的帖子
     */
    @Schema(description = "帖子ID")
    private Long postId;

    @Serial
    private static final long serialVersionUID = 1L;
}
