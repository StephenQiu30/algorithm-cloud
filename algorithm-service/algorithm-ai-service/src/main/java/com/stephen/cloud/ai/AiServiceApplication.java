package com.stephen.cloud.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * AI 服务启动类
 *
 * @author StephenQiu30
 */
@SpringBootApplication(scanBasePackages = { "com.stephen.cloud.ai", "com.stephen.cloud.common" })
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.stephen.cloud.api")
@MapperScan("com.stephen.cloud.ai.mapper")
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }

}
