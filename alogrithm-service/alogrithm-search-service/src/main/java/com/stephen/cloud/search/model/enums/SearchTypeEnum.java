package com.stephen.cloud.search.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索类型枚举
 *
 * @author stephen
 */
@Getter
@AllArgsConstructor
public enum SearchTypeEnum {

    /**
     * 帖子
     */
    POST("帖子", "post"),

    /**
     * 用户
     */
    USER("用户", "user");

    /**
     * 文案
     */
    private final String text;

    /**
     * 值
     */
    private final String value;

    /**
     * 获取值列表
     *
     * @return {@link List<String>}
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value value
     * @return {@link SearchTypeEnum}
     */
    public static SearchTypeEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (SearchTypeEnum anEnum : SearchTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
