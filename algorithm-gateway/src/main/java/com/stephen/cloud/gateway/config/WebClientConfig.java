package com.stephen.cloud.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient 配置类
 * <p>
 * 提供支持 LoadBalancer 的 {@link WebClient.Builder}，
 * 用于网关内部通过服务名调用下游微服务（如日志服务）。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
