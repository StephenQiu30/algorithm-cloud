package com.stephen.cloud.common.rabbitmq.enums;

import lombok.Getter;

/**
 * ES 同步数据类型枚举
 *
 * @author StephenQiu30
 */
@Getter
public enum EsSyncDataTypeEnum {

    POST("post"),
    USER("user"),
    CHUNK("chunk");

    private final String value;

    EsSyncDataTypeEnum(String value) {
        this.value = value;
    }

    /**
     * 根据值获取枚举
     *
     * @param value 值
     * @return 枚举
     */
    public static EsSyncDataTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (EsSyncDataTypeEnum typeEnum : EsSyncDataTypeEnum.values()) {
            if (typeEnum.value.equalsIgnoreCase(value) || typeEnum.name().equalsIgnoreCase(value)) {
                return typeEnum;
            }
        }
        return null;
    }
}
