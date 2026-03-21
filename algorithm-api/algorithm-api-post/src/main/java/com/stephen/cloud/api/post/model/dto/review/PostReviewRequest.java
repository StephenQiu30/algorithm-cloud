package com.stephen.cloud.api.post.model.dto.review;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 帖子审核请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "帖子审核请求")
public class PostReviewRequest implements Serializable {

    /**
     * 帖子 ID
     */
    @Schema(description = "帖子 ID")
    private Long id;

    /**
     * 审核状态：1-通过，2-拒绝
     */
    @Schema(description = "审核状态：1-通过，2-拒绝")
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    @Schema(description = "审核信息")
    private String reviewMessage;

    @Serial
    private static final long serialVersionUID = 1L;
}
