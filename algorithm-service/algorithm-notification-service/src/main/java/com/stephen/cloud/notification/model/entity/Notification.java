package com.stephen.cloud.notification.model.entity;

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
 * 通知实体
 * <p>
 * 支持系统通知、用户通知、评论通知、点赞通知
 * 支持全员广播、按角色发送、按用户列表发送
 * 支持消息去重（通过bizId实现幂等）
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@TableName("notification")
@Schema(description = "通知表")
public class Notification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 通知ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "通知ID")
    private Long id;

    /**
     * 通知标题
     */
    @Schema(description = "通知标题")
    private String title;

    /**
     * 通知内容
     */
    @Schema(description = "通知内容")
    private String content;

    /**
     * 通知类型（system-系统通知，user-用户通知，comment-评论通知，like-点赞通知，follow-关注通知，broadcast-全员广播）
     */
    @Schema(description = "通知类型（system-系统通知，user-用户通知，comment-评论通知，like-点赞通知，follow-关注通知，broadcast-全员广播）")
    private String type;

    /**
     * 业务幂等ID
     */
    @Schema(description = "业务幂等ID")
    private String bizId;

    /**
     * 接收用户ID
     */
    @Schema(description = "接收用户ID")
    private Long userId;

    /**
     * 关联对象ID
     */
    @Schema(description = "关联对象ID")
    private Long relatedId;

    /**
     * 关联对象类型
     */
    @Schema(description = "关联对象类型")
    private String relatedType;

    /**
     * 是否已读
     */
    @Schema(description = "是否已读")
    private Integer isRead;

    /**
     * 状态（0-正常，1-停用）
     */
    @Schema(description = "状态（0-正常，1-停用）")
    private Integer status;

    /**
     * 跳转链接
     */
    @Schema(description = "跳转链接")
    private String contentUrl;

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
