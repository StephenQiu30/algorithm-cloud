package com.stephen.cloud.api.search.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * 搜索类型枚举
 *
 * @author StephenQiu30
 */
@Getter
public enum SearchTypeEnum {

    POST("post", "帖子"),
    USER("user", "用户"),
    PICTURE("picture", "图片");

    /**
     * 枚举值
     */
    private final String value;

    /**
     * 枚举文本
     */
    private final String text;

    SearchTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 值
     * @return 枚举
     */
    public static SearchTypeEnum getEnumByValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        for (SearchTypeEnum searchTypeEnum : SearchTypeEnum.values()) {
            if (searchTypeEnum.value.equals(value)) {
                return searchTypeEnum;
            }
        }
        return null;
    }
}
