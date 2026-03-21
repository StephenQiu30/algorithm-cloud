package com.stephen.cloud.api.notification.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知类型枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum NotificationTypeEnum {

    /**
     * 系统通知
     */
    SYSTEM("system", "系统通知"),

    /**
     * 用户通知
     */
    USER("user", "用户通知"),

    /**
     * 评论通知
     */
    COMMENT("comment", "评论通知"),

    /**
     * 点赞通知
     */
    LIKE("like", "点赞通知"),

    /**
     * 收藏通知
     */
    FAVOUR("favour", "收藏通知"),

    /**
     * 关注通知
     */
    FOLLOW("follow", "关注通知"),

    /**
     * 审核通知
     */
    POST_REVIEW("post_review", "审核通知"),

    /**
     * 全员广播
     */
    BROADCAST("broadcast", "全员广播");

    /**
     * 类型编码
     */
    private final String code;

    /**
     * 类型描述
     */
    private final String desc;

    /**
     * 获取值列表
     *
     * @return {@link List<String>}
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.code).collect(Collectors.toList());
    }

    /**
     * 根据 code 获取枚举
     *
     * @param code 类型编码
     * @return {@link NotificationTypeEnum}
     */
    public static NotificationTypeEnum getEnumByCode(String code) {
        if (ObjectUtils.isEmpty(code)) {
            return null;
        }
        for (NotificationTypeEnum typeEnum : NotificationTypeEnum.values()) {
            if (typeEnum.code.equals(code)) {
                return typeEnum;
            }
        }
        return null;
    }

}
