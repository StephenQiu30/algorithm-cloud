package com.stephen.cloud.api.notification.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知读取状态枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum NotificationStatusEnum {

    /**
     * 未读
     */
    UNREAD("未读", 0),
    /**
     * 已读
     */
    READ("已读", 1);

    /**
     * 状态文案
     */
    private final String text;

    /**
     * 状态值
     */
    private final Integer value;

    /**
     * 获取值列表
     *
     * @return List<Integer>
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value value
     * @return {@link NotificationStatusEnum}
     */
    public static NotificationStatusEnum getEnumByValue(Integer value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (NotificationStatusEnum anEnum : NotificationStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

}
