package com.stephen.cloud.common.cache.utils.lock.function;

/**
 * 无返回值的函数式接口
 *
 * @author StephenQiu30
 */
@FunctionalInterface
public interface VoidFunction {
    /**
     * 执行方法
     */
    void method();
}
