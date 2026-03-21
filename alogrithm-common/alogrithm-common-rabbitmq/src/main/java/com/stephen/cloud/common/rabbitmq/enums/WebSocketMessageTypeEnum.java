package com.stephen.cloud.common.rabbitmq.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WebSocket 消息类型枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum WebSocketMessageTypeEnum {

    /**
     * 认证消息
     */
    AUTH(0, "认证"),

    /**
     * 心跳消息
     */
    HEARTBEAT(1, "心跳"),

    /**
     * 普通消息
     */
    MESSAGE(2, "消息"),

    /**
     * 系统通知
     */
    SYSTEM_NOTICE(3, "系统通知"),

    /**
     * 评论通知
     */
    COMMENT_NOTICE(4, "评论通知"),

    /**
     * 点赞通知
     */
    THUMB_NOTICE(5, "点赞通知"),

    /**
     * 收藏通知
     */
    FAVOUR_NOTICE(6, "收藏通知"),

    /**
     * 关注通知
     */
    FOLLOW_NOTICE(7, "关注通知"),

    /**
     * 在线状态
     */
    ONLINE_STATUS(8, "在线状态"),

    /**
     * 错误消息
     */
    ERROR(99, "错误");

    /**
     * 编码
     */
    private final Integer code;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 获取值列表
     *
     * @return {@link List<Integer>}
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.code).collect(Collectors.toList());
    }

    /**
     * 根据 code 获取枚举
     *
     * @param code code
     * @return {@link WebSocketMessageTypeEnum}
     */
    public static WebSocketMessageTypeEnum getEnumByCode(Integer code) {
        if (ObjectUtils.isEmpty(code)) {
            return null;
        }
        for (WebSocketMessageTypeEnum typeEnum : WebSocketMessageTypeEnum.values()) {
            if (typeEnum.code.equals(code)) {
                return typeEnum;
            }
        }
        return null;
    }

}
