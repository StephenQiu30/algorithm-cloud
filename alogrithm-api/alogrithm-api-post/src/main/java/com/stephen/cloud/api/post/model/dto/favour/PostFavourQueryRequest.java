package com.stephen.cloud.api.post.model.dto.favour;

import com.stephen.cloud.api.post.model.dto.post.PostQueryRequest;
import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 帖子收藏查询请求
 *
 * @author StephenQiu30
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "帖子收藏查询请求")
public class PostFavourQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 帖子查询条件
     */
    @Schema(description = "帖子查询条件")
    private PostQueryRequest postQueryRequest;
}
