package com.stephen.cloud.api.post.model.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 更新帖子请求（管理员）
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "帖子更新请求")
public class PostUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 帖子ID
     */
    @Schema(description = "帖子ID")
    private Long id;

    /**
     * 标题
     */
    @Schema(description = "标题")
    private String title;

    /**
     * 内容
     */
    @Schema(description = "内容")
    private String content;

    /**
     * 封面
     */
    @Schema(description = "封面")
    private String cover;

    /**
     * 标签列表
     */
    @Schema(description = "标签列表")
    private List<String> tags;

    /**
     * 内容类型(0-帖子 1-算法知识库)
     */
    @Schema(description = "内容类型(0-帖子 1-算法知识库)")
    private Integer contentType;
}
