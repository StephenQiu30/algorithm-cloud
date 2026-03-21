package com.stephen.cloud.common.utils;

import java.util.regex.Pattern;

/**
 * 正则表达式工具类
 *
 * @author StephenQiu30
 */
public class RegexUtils {

    /**
     * 邮箱正则表达式
     */
    public static final String EMAIL_REGEX = "^[A-Za-z0-9\\u4e00-\\u9fa5]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";

    /**
     * 手机号正则表达式
     */
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    /**
     * URL 正则表达式
     */
    public static final String URL_REGEX = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$";

    /**
     * 身份证正则表达式 (基础 18 位)
     */
    public static final String ID_CARD_REGEX = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$";

    /**
     * 数字正则表达式
     */
    public static final String NUMBER_REGEX = "^\\d+$";

    /**
     * 密码正则表达式 (6-20 位，必须包含字母和数字)
     */
    public static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-zA-Z]).{6,20}$";

    /**
     * 校验邮箱
     *
     * @param email 邮箱
     * @return 是否符合正则
     */
    public static boolean checkEmail(String email) {
        return Pattern.matches(EMAIL_REGEX, email);
    }

    /**
     * 校验手机号
     *
     * @param phone 手机号
     * @return 是否符合正则
     */
    public static boolean checkPhone(String phone) {
        return Pattern.matches(PHONE_REGEX, phone);
    }

    /**
     * 校验 URL
     */
    public static boolean checkUrl(String url) {
        return Pattern.matches(URL_REGEX, url);
    }

    /**
     * 校验身份证
     */
    public static boolean checkIdCard(String idCard) {
        return Pattern.matches(ID_CARD_REGEX, idCard);
    }

    /**
     * 校验是否为纯数字
     */
    public static boolean isNumber(String value) {
        return Pattern.matches(NUMBER_REGEX, value);
    }

    /**
     * 校验密码强度
     */
    public static boolean checkPassword(String password) {
        return Pattern.matches(PASSWORD_REGEX, password);
    }
}
