package com.stephen.cloud.api.notification.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum NotificationRelatedTypeEnum {

    POST("post", "帖子"),
    USER("user", "用户"),
    COMMENT("comment", "评论");

    private final String value;
    private final String desc;

    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public static NotificationRelatedTypeEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (NotificationRelatedTypeEnum typeEnum : NotificationRelatedTypeEnum.values()) {
            if (typeEnum.value.equals(value)) {
                return typeEnum;
            }
        }
        return null;
    }
}

