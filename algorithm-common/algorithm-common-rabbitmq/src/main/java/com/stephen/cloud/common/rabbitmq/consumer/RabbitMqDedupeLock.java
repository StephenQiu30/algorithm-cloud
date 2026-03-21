package com.stephen.cloud.common.rabbitmq.consumer;

import java.lang.annotation.*;

/**
 * RabbitMQ 消费去重锁注解（SETNX）
 * <p>
 * 标记在 {@link RabbitMqHandler} 实现类上，开启消费端基于缓存的去重锁，避免重复消费导致的副作用。
 * </p>
 *
 * @author StephenQiu30
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RabbitMqDedupeLock {

    /**
     * 缓存 Key 前缀
     */
    String prefix() default "mq:dedupe:";

    /**
     * 过期时间（秒），默认 24 小时
     */
    int expire() default 86400;
}

