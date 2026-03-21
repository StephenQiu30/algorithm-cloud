package com.stephen.cloud.common.rabbitmq.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * WebSocket 推送类型枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum WebSocketPushTypeEnum {

    /**
     * 单用户
     */
    SINGLE("single", "单用户"),

    /**
     * 多用户
     */
    MULTIPLE("multiple", "多用户"),

    /**
     * 广播
     */
    BROADCAST("broadcast", "广播");

    /**
     * 值
     */
    private final String value;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 获取枚举
     *
     * @param value 值
     * @return {@link WebSocketPushTypeEnum}
     */
    public static WebSocketPushTypeEnum getEnumByValue(String value) {
        for (WebSocketPushTypeEnum typeEnum : WebSocketPushTypeEnum.values()) {
            if (typeEnum.value.equals(value)) {
                return typeEnum;
            }
        }
        return null;
    }
}
