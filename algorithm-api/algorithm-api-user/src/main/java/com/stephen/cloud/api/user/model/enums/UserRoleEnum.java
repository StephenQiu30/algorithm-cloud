package com.stephen.cloud.api.user.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户角色枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum UserRoleEnum {

    /**
     * 普通用户
     */
    USER("用户", "user"),
    /**
     * 管理员
     */
    ADMIN("管理员", "admin"),
    /**
     * 被封号用户
     */
    BAN("被封号", "ban");

    /**
     * 角色文案
     */
    private final String text;

    /**
     * 角色值
     */
    private final String value;

    /**
     * 获取值列表
     *
     * @return List<String>
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value value
     * @return {@link UserRoleEnum}
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

}
