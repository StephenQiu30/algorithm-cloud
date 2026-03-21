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
 * 帖子评论实体
 * <p>
 * 支持一级评论和二级评论（回复）
 * 二级评论的parentId指向一级评论ID
 * </p>
 *
 * @author StephenQiu30
 */
@TableName(value = "post_comment")
@Data
@Schema(description = "帖子评论表")
public class PostComment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "评论ID")
    private Long id;

    /**
     * 评论内容
     */
    @Schema(description = "评论内容")
    private String content;

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
     * 父评论ID（0表示一级评论）
     */
    @Schema(description = "父评论ID（0表示一级评论）")
    private Long parentId;

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