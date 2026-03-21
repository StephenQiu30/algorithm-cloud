package com.stephen.cloud.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * 限流配置
 * <p>
 * 提供限流所需的 {@link KeyResolver} Bean，
 * 支持按 IP、用户ID、API 路径三种维度进行限流。
 * </p>
 *
 * @author StephenQiu30
 */
@Configuration
public class RateLimitConfig {

    /**
     * IP 限流 Key 解析器（默认）
     * <p>
     * 当客户端 IP 无法解析时，降级使用 "unknown" 作为限流 Key
     * </p>
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            var remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                return Mono.just(remoteAddress.getAddress().getHostAddress());
            }
            return Mono.just("unknown");
        };
    }

    /**
     * 用户 ID 限流 Key 解析器
     * <p>
     * 从请求头中获取 userId，未登录用户统一使用 "anonymous" 限流
     * </p>
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("userId");
            return Mono.just(userId != null ? userId : "anonymous");
        };
    }

    /**
     * API 路径限流 Key 解析器
     */
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getPath().value());
    }
}
