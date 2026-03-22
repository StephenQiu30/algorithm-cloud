package com.stephen.cloud.post.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 帖子实体
 * <p>
 * 支持帖子发布、编辑、删除、点赞、收藏功能
 * 支持标签分类和搜索
 * </p>
 *
 * @author StephenQiu30
 */
@TableName(value = "post")
@Data
@Schema(description = "帖子表")
public class Post implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 帖子ID
     */
    @TableId(type = IdType.ASSIGN_ID)
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
     * 标签列表（JSON）
     */
    @Schema(description = "标签列表（JSON）")
    private String tags;

    /**
     * 点赞数
     */
    @Schema(description = "点赞数")
    private Integer thumbNum;

    /**
     * 收藏数
     */
    @Schema(description = "收藏数")
    private Integer favourNum;

    /**
     * 创建用户ID
     */
    @Schema(description = "创建用户ID")
    private Long userId;

    /**
     * 审核状态(0-待审核 1-通过 2-拒绝)
     */
    @Schema(description = "审核状态(0-待审核 1-通过 2-拒绝)")
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    @Schema(description = "审核信息")
    private String reviewMessage;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    @Schema(description = "是否删除")
    private Integer isDelete;
}
