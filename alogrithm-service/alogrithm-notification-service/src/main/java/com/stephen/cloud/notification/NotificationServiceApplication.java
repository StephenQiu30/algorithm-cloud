package com.stephen.cloud.notification;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 通知服务启动类
 *
 * @author StephenQiu30
 */
@SpringBootApplication(scanBasePackages = { "com.stephen.cloud.notification", "com.stephen.cloud.common" })
@MapperScan("com.stephen.cloud.notification.mapper")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.stephen.cloud.api")
@EnableAsync
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
