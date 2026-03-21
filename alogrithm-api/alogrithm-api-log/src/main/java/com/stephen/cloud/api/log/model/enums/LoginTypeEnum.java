package com.stephen.cloud.api.log.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录类型枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum LoginTypeEnum {

    /**
     * GitHub 登录
     */
    GITHUB("GITHUB", "GitHub 登录"),

    /**
     * 邮箱登录
     */
    EMAIL("EMAIL", "邮箱登录"),

    /**
     * 微信公众号登录
     */
    WECHAT_MP("WECHAT_MP", "微信公众号登录"),

    /**
     * 微信扫码登录
     */
    WECHAT_SCAN("WECHAT_SCAN", "微信扫码登录");

    private final String value;
    private final String text;
}
