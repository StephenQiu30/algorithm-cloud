package com.stephen.cloud.api.post.model.dto.post;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 帖子查询请求
 *
 * @author StephenQiu30
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "帖子查询请求")
public class PostQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 帖子ID
     */
    @Schema(description = "帖子ID")
    private Long id;

    /**
     * 排除的帖子ID
     */
    @Schema(description = "排除的帖子ID")
    private Long notId;

    /**
     * 搜索词
     */
    @Schema(description = "搜索词")
    private String searchText;

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
     * 标签列表
     */
    @Schema(description = "标签列表")
    private List<String> tags;

    /**
     * 至少有一个标签
     */
    @Schema(description = "至少有一个标签")
    private List<String> orTags;

    /**
     * 创建用户ID
     */
    @Schema(description = "创建用户ID")
    private Long userId;

    /**
     * 收藏用户ID
     */
    @Schema(description = "收藏用户ID")
    private Long favourUserId;

    /**
     * 审核状态（0-待审核，1-通过，2-拒绝）
     */
    @Schema(description = "审核状态")
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    @Schema(description = "审核信息")
    private String reviewMessage;

    /**
     * 内容类型(0-帖子 1-算法知识库)
     */
    @Schema(description = "内容类型(0-帖子 1-算法知识库)")
    private Integer contentType;
}
