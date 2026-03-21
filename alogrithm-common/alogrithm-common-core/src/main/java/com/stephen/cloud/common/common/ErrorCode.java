package com.stephen.cloud.common.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义错误码
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    /**
     * 成功
     */
    SUCCESS(0, "ok"),

    /**
     * 请求参数错误
     */
    PARAMS_ERROR(40000, "请求参数错误"),

    /**
     * 处理excel文件错误
     */
    EXCEL_ERROR(40001, "处理excel文件错误, 请检查表格信息是否有误"),

    /**
     * 上传图片大小最大为5MB
     */
    PARAMS_SIZE_ERROR(40002, "上传图片大小最大为5MB"),

    /**
     * 处理word文件错误
     */
    WORD_ERROR(40003, "处理word文件错误"),

    /**
     * AI识别失败
     */
    AI_ERROR(40004, "AI识别失败"),

    /**
     * 验证码错误，或已失效
     */
    CAPTCHA_ERROR(40005, "验证码错误，或已失效"),

    /**
     * 未登录
     */
    NOT_LOGIN_ERROR(40100, "未登录"),

    /**
     * 无权限
     */
    NO_AUTH_ERROR(40101, "无权限"),

    /**
     * 请求数据不存在
     */
    NOT_FOUND_ERROR(40400, "请求数据不存在"),

    /**
     * 请求太频繁
     */
    TOO_FREQUENT(40401, "请求太频繁"),

    /**
     * 请求过于频繁
     */
    TOO_MANY_REQUEST(40429, "请求过于频繁，请稍后再试"),

    /**
     * 禁止访问
     */
    FORBIDDEN_ERROR(40300, "禁止访问"),

    /**
     * 已存在
     */
    ALREADY_EXIST(40301, "已存在"),

    /**
     * 系统内部异常
     */
    SYSTEM_ERROR(50000, "系统内部异常"),

    /**
     * 操作失败
     */
    OPERATION_ERROR(50001, "操作失败"),

    /**
     * 服务暂时不可用
     */
    SERVICE_UNAVAILABLE(50002, "服务暂时不可用");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    /**
     * 获取值列表
     *
     * @return {@link List<Integer>}
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.code).collect(Collectors.toList());
    }

    /**
     * 根据 code 获取枚举
     *
     * @param code 状态码
     * @return {@link ErrorCode}
     */
    public static ErrorCode getEnumByCode(int code) {
        if (ObjectUtils.isEmpty(code)) {
            return null;
        }
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return null;
    }

}
