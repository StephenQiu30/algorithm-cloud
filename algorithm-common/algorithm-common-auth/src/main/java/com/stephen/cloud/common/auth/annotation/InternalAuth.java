package com.stephen.cloud.common.auth.annotation;

import java.lang.annotation.*;

/**
 * 内部调用认证注解
 * 用于标识仅允许内部调用的接口
 * 配合 InternalAuthAspect 使用，检查请求头中是否包含指定的内部调用标识
 *
 * @author StephenQiu30
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InternalAuth {
}
