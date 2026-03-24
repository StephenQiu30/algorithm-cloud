package com.stephen.cloud.common.rabbitmq.consumer;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import jakarta.annotation.Resource;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * RabbitMQ 消费去重存储（SETNX）
 * <p>
 * MVP：仅提供消费幂等所需的 “不存在则写入” 能力，避免把中间件特性下沉到 cache 通用模块。
 * </p>
 */
@Component
public class RabbitMqDedupeStore {

    @Resource
    private RedissonClient redissonClient;

    public boolean putIfAbsent(String key, int expireSeconds) {
        int finalExpire = expireSeconds + RandomUtil.randomInt(0, 600);
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        return BooleanUtil.isTrue(bucket.setIfAbsent("1", Duration.ofSeconds(finalExpire)));
    }
}

