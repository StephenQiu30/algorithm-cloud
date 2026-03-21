package com.stephen.cloud.api.notification.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知目标类型枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum NotificationTargetTypeEnum {

    /**
     * 全员广播
     */
    ALL("all", "所有人"),

    /**
     * 指定角色
     */
    ROLE("@role:", "按角色"),

    /**
     * 指定用户列表
     */
    USER_IDS("user_ids", "按用户ID列表");

    /**
     * 值
     */
    private final String value;

    /**
     * 描述
     */
    private final String desc;
}
