package com.stephen.cloud.common.cache.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.stephen.cloud.common.cache.properties.CaffeineProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置
 *
 * @author StephenQiu30
 */
@Configuration
@Slf4j
@ConditionalOnProperty(prefix = "caffeine", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CaffeineConfiguration {

    @Resource
    private CaffeineProperties caffeineProperties;

    @Bean("localCache")
    public Cache<String, Object> localCache() {
        return Caffeine.newBuilder()
                // 写入后过期
                .expireAfterWrite(caffeineProperties.getExpired(), TimeUnit.SECONDS)
                // 访问后过期
                .expireAfterAccess(caffeineProperties.getExpired(), TimeUnit.SECONDS)
                // 初始容量
                .initialCapacity(caffeineProperties.getInitCapacity())
                // 最大容量
                .maximumSize(caffeineProperties.getMaxCapacity())
                .build();
    }

    /**
     * 依赖注入日志输出
     */
    @PostConstruct
    private void initDi() {
        log.info("############ {} Configuration DI.", this.getClass().getSimpleName().split("\\$\\$")[0]);
    }
}
