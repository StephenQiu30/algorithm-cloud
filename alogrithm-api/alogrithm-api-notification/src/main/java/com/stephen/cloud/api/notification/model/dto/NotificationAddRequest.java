package com.stephen.cloud.api.notification.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建通知请求 (管理员智能创建)
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "创建通知请求")
public class NotificationAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 发送目标标识
     * 支持：
     * 1. "all" - 全员广播
     * 2. "@role:xxx" - 按角色发送（如 @role:admin）
     * 3. "1,2,3" - 逗号分隔的用户ID列表
     */
    @Schema(description = "发送目标标识")
    private String target;

    /**
     * 通知标题
     * 可选，不填则根据内容自动生成
     */
    @Schema(description = "通知标题")
    private String title;

    /**
     * 通知内容
     * 必填
     */
    @Schema(description = "通知内容")
    private String content;

    /**
     * 跳转链接
     * 可选，系统将自动尝试解析关联业务ID和类型
     */
    @Schema(description = "跳转链接")
    private String contentUrl;

}
