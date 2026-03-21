package com.stephen.cloud.api.mail.model.enums;

import lombok.Getter;

@Getter
public enum EmailStatusEnum {

    PENDING("PENDING", "待发送"),
    SUCCESS("SUCCESS", "发送成功"),
    FAILED("FAILED", "发送失败"),
    CANCELLED("CANCELLED", "业务取消");

    private final String value;
    private final String description;

    EmailStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static EmailStatusEnum getEnumByValue(String value) {
        for (EmailStatusEnum statusEnum : values()) {
            if (statusEnum.getValue().equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
