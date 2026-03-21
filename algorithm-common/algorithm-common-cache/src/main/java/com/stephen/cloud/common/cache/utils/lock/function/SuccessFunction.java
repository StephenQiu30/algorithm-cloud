package com.stephen.cloud.common.cache.utils.lock.function;

/**
 * 成功获取锁后的函数式接口
 *
 * @author StephenQiu30
 */
@FunctionalInterface
public interface SuccessFunction {
    /**
     * 执行方法
     */
    void method();
}
