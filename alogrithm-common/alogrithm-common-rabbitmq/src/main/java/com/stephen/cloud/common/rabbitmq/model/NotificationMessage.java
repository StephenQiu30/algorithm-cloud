package com.stephen.cloud.common.rabbitmq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通知消息模型（MQ）
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务ID（用于幂等性控制）
     */
    private String bizId;

    /**
     * 通知 ID
     */
    private Long notificationId;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 通知类型
     */
    private String type;

    /**
     * 接收用户 ID
     */
    private Long userId;

    /**
     * 关联对象 ID
     */
    private Long relatedId;

    /**
     * 关联对象类型
     */
    private String relatedType;
}
