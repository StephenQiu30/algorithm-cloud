package com.stephen.cloud.api.mail.model.enums;

import lombok.Getter;

/**
 * 邮件状态枚举
 * <p>
 * 定义邮件发送的生命周期状态，用于追踪邮件发送结果。
 * </p>
 *
 * @author StephenQiu30
 */
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
