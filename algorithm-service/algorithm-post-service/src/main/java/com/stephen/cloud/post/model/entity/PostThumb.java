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
 * 帖子点赞实体
 * <p>
 * 记录用户对帖子的点赞记录
 * 同一用户对同一帖子只能点赞一次
 * 再次点赞会取消点赞
 * </p>
 *
 * @author StephenQiu30
 */
@TableName(value = "post_thumb")
@Data
@Schema(description = "帖子点赞表")
public class PostThumb implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 点赞ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "点赞ID")
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
