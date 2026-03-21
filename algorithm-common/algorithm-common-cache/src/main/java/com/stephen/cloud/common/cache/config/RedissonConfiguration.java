package com.stephen.cloud.common.cache.config;

import com.stephen.cloud.common.cache.properties.RedisProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置类
 * 复用 Spring Data Redis 配置，自动创建 RedissonClient
 *
 * @author StephenQiu30
 */
@Configuration
@Slf4j
@ConditionalOnProperty(prefix = "spring.data.redis", name = "host")
public class RedissonConfiguration {

    @Resource
    private RedisProperties redisProperties;

    /**
     * 创建 RedissonClient 单例 Bean
     * 用于提供分布式锁、缓存等 Redisson 操作接口
     *
     * @return RedissonClient 实例
     */
    @Bean("redissonClient")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = String.format("redis://%s:%d", redisProperties.getHost(), redisProperties.getPort());

        config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisProperties.getDatabase())
                .setConnectionMinimumIdleSize(5)
                .setConnectionPoolSize(10)
                .setConnectTimeout(redisProperties.getTimeout())
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            config.useSingleServer().setPassword(redisProperties.getPassword());
        }

        return Redisson.create(config);
    }

    /**
     * 依赖注入日志输出
     */
    @PostConstruct
    private void initDi() {
        log.info("############ {} Configuration DI.", this.getClass().getSimpleName().split("\\$\\$")[0]);
    }
}
