package com.stephen.cloud.common.common;

import com.stephen.cloud.common.exception.BusinessException;

/**
 * 抛异常工具类
 *
 * @author StephenQiu30
 */
public class ThrowUtils {

    /**
     * 条件成立则抛出指定的运行时异常
     *
     * @param condition        抛出异常的条件
     * @param runtimeException 要抛出的运行时异常
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件成立则抛出业务异常 (BusinessException)
     *
     * @param condition 条件
     * @param errorCode 错误码枚举
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 条件成立则抛出业务异常 (BusinessException)，带自定义消息
     *
     * @param condition 条件
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
