package com.stephen.cloud.post.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 帖子收藏实体
 * <p>
 * 记录用户对帖子的收藏记录
 * 同一用户对同一帖子只能收藏一次
 * 再次收藏会取消收藏
 * </p>
 *
 * @author StephenQiu30
 */
@TableName(value = "post_favour")
@Data
@Schema(description = "帖子收藏表")
public class PostFavour implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 收藏ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "收藏ID")
    private Long id;

    /**
     * 帖子ID
     */
    @Schema(description = "帖子ID")
    private Long postId;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

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
}
