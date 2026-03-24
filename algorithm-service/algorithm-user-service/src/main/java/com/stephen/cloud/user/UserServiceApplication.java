package com.stephen.cloud.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 用户服务启动类
 *
 * @author StephenQiu30
 */
@SpringBootApplication(scanBasePackages = {"com.stephen.cloud.user", "com.stephen.cloud.common"})
@MapperScan("com.stephen.cloud.user.mapper")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.stephen.cloud.api"})
@EnableAsync
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
