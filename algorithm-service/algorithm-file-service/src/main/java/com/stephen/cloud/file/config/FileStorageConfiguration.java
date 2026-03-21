package com.stephen.cloud.file.config;

import com.stephen.cloud.file.config.properties.FileStorageProperties;
import com.stephen.cloud.file.manager.CosManager;
import com.stephen.cloud.file.service.FileStorageService;
import com.stephen.cloud.file.service.impl.CosFileStorageServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储配置类
 *
 * @author StephenQiu30
 */
@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "cos")
    public FileStorageService cosFileStorageService(CosManager cosManager) {
        return new CosFileStorageServiceImpl(cosManager);
    }
}
