package com.stephen.cloud.log;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 日志服务启动类
 *
 * @author StephenQiu30
 */
@SpringBootApplication(scanBasePackages = { "com.stephen.cloud.log", "com.stephen.cloud.common" })
@MapperScan("com.stephen.cloud.log.mapper")
@EnableDiscoveryClient
@EnableScheduling
public class LogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogServiceApplication.class, args);
    }
}
