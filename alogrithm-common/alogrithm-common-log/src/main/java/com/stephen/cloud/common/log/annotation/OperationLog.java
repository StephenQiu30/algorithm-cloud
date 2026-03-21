package com.stephen.cloud.common.log.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于自动记录Controller方法的操作日志
 * <p>
 * 使用示例：
 *
 * <pre>
 * {@code
 * &#64;PostMapping("/update")
 * @OperationLog(module = "用户管理", action = "更新用户")
 * public BaseResponse<Boolean> updateUser(...) {
 *     // 业务逻辑
 * }
 * }
 * </pre>
 *
 * @author StephenQiu30
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作模块
     * 例如："用户管理"、"帖子管理"、"文件管理"等
     */
    String module();

    /**
     * 操作类型/动作
     * 例如："创建用户"、"更新帖子"、"删除文件"等
     */
    String action();

    /**
     * 是否记录请求参数
     * 默认true
     */
    boolean recordParams() default true;

    /**
     * 是否记录响应结果
     * 默认false（避免记录大量数据）
     */
    boolean recordResult() default false;
}
