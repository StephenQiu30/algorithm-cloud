package com.stephen.cloud.api.ai.model.enums;

import lombok.Getter;

/**
 * 文档解析状态枚举
 *
 * @author StephenQiu30
 */
@Getter
public enum DocumentParseStatusEnum {

    PENDING("PENDING", "待解析"),
    PROCESSING("PROCESSING", "解析中"),
    COMPLETED("COMPLETED", "解析完成"),
    FAILED("FAILED", "解析失败"),
    TIMEOUT("TIMEOUT", "解析超时");

    private final String value;
    private final String description;

    DocumentParseStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static DocumentParseStatusEnum getEnumByValue(String value) {
        for (DocumentParseStatusEnum statusEnum : values()) {
            if (statusEnum.getValue().equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
