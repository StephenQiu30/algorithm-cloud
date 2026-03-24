package com.stephen.cloud.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 文件服务启动类
 *
 * @author StephenQiu30
 */
@SpringBootApplication(scanBasePackages = {"com.stephen.cloud.file", "com.stephen.cloud.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.stephen.cloud.api")
@EnableAsync
public class FileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileServiceApplication.class, args);
    }

}
