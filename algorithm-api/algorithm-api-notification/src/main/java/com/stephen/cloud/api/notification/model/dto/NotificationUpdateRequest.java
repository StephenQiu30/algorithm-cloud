package com.stephen.cloud.api.notification.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新通知请求（管理员）
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "更新通知请求")
public class NotificationUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 通知ID
     */
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
     * 通知类型
     */
    @Schema(description = "通知类型")
    private String type;

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
     * 跳转链接
     */
    @Schema(description = "跳转链接")
    private String contentUrl;
}
