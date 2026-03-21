package com.stephen.cloud.api.log.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录状态枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum LoginStatusEnum {

    /**
     * 登录成功
     */
    SUCCESS("SUCCESS", "登录成功"),

    /**
     * 登录失败
     */
    FAILED("FAILED", "登录失败");

    private final String value;
    private final String text;
}
