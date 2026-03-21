package com.stephen.cloud.api.file.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件存储类型枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum StorageTypeEnum {

    /**
     * 腾讯云对象存储
     */
    COS("COS", "腾讯云对象存储"),

    /**
     * MinIO对象存储
     */
    MINIO("MINIO", "MinIO对象存储"),

    /**
     * 阿里云对象存储
     */
    OSS("OSS", "阿里云对象存储");

    /**
     * 类型值
     */
    private final String value;

    /**
     * 描述
     */
    private final String description;

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
     * @return {@link StorageTypeEnum}
     */
    public static StorageTypeEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (StorageTypeEnum typeEnum : StorageTypeEnum.values()) {
            if (typeEnum.value.equalsIgnoreCase(value)) {
                return typeEnum;
            }
        }
        return null;
    }

}
