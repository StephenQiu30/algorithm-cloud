package com.stephen.cloud.mail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 邮件服务启动类
 *
 * @author StephenQiu30
 */
@SpringBootApplication(scanBasePackages = { "com.stephen.cloud.mail", "com.stephen.cloud.common" })
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.stephen.cloud.api")
@EnableAsync
public class MailServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailServiceApplication.class, args);
    }
}
