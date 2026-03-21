package com.stephen.cloud.api.post.model.dto.post;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 编辑帖子请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "编辑请求")
public class PostEditRequest implements Serializable {

    /**
     * 帖子ID
     * 必填，指定要编辑的帖子
     */
    @Schema(description = "帖子ID")
    private Long id;

    /**
     * 标题
     * 可选，帖子标题
     */
    @Schema(description = "标题")
    private String title;

    /**
     * 内容
     * 可选，帖子正文内容
     */
    @Schema(description = "内容")
    private String content;

    /**
     * 封面
     * 可选，帖子封面图片URL
     */
    @Schema(description = "封面")
    private String cover;

    /**
     * 标签列表
     * 可选，用于分类和搜索
     */
    @Schema(description = "标签列表")
    private List<String> tags;

    /**
     * 内容类型(0-帖子 1-算法知识库)
     */
    @Schema(description = "内容类型(0-帖子 1-算法知识库)")
    private Integer contentType;

    @Serial
    private static final long serialVersionUID = 1L;
}
