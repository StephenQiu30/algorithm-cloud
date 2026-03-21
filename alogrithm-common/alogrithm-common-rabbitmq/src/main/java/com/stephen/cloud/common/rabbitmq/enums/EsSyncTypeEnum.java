package com.stephen.cloud.common.rabbitmq.enums;

import lombok.Getter;

/**
 * ES 同步方式枚举
 *
 * @author StephenQiu30
 */
@Getter
public enum EsSyncTypeEnum {

    FULL("FULL"),
    INC("INC");

    private final String value;

    EsSyncTypeEnum(String value) {
        this.value = value;
    }

    /**
     * 根据值获取枚举
     *
     * @param value 值
     * @return 枚举
     */
    public static EsSyncTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (EsSyncTypeEnum typeEnum : EsSyncTypeEnum.values()) {
            if (typeEnum.value.equalsIgnoreCase(value) || typeEnum.name().equalsIgnoreCase(value)) {
                return typeEnum;
            }
        }
        return null;
    }
}
