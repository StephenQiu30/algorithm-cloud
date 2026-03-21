package com.stephen.cloud.api.user.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户邮箱验证状态枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum EmailVerifiedEnum {

    /**
     * 未验证
     */
    UNVERIFIED("未验证", 0),
    /**
     * 已验证
     */
    VERIFIED("已验证", 1);

    /**
     * 状态文案
     */
    private final String text;

    /**
     * 状态值
     */
    private final Integer value;

    /**
     * 获取值列表
     *
     * @return List<Integer>
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value value
     * @return {@link EmailVerifiedEnum}
     */
    public static EmailVerifiedEnum getEnumByValue(Integer value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (EmailVerifiedEnum anEnum : EmailVerifiedEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

}
