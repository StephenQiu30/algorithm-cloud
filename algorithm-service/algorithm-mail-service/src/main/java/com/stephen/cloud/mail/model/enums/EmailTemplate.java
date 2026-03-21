package com.stephen.cloud.mail.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 邮件模板枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum EmailTemplate {

    /**
     * 验证码邮件模板
     */
    VERIFICATION_CODE("email/verification-code", "验证码"),

    /**
     * 欢迎邮件模板
     */
    WELCOME("email/welcome", "欢迎邮件"),

    /**
     * 密码重置邮件模板
     */
    PASSWORD_RESET("email/password-reset", "密码重置"),

    /**
     * 通知邮件模板
     */
    NOTIFICATION("email/notification", "系统通知");

    /**
     * 模板路径（相对于 templates 目录）
     */
    private final String templatePath;

    /**
     * 模板描述
     */
    private final String description;
}
