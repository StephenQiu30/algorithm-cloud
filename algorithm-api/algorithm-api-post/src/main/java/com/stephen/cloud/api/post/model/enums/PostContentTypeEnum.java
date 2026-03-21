package com.stephen.cloud.api.post.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;

@Getter
public enum PostContentTypeEnum {

    POST(0),
    ALGO_KB(1);

    private final Integer value;

    PostContentTypeEnum(Integer value) {
        this.value = value;
    }

    public static PostContentTypeEnum getByValue(Integer value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        return Arrays.stream(values()).filter(e -> e.value.equals(value)).findFirst().orElse(null);
    }
}
