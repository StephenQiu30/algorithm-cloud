package com.stephen.cloud.file.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import com.stephen.cloud.file.config.properties.CosProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 COS 配置
 *
 * @author StephenQiu30
 */
@Configuration
@EnableConfigurationProperties(CosProperties.class)
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "cos")
@Slf4j
public class CosConfiguration {

    @Resource
    private CosProperties cosProperties;

    @Bean
    public COSClient cosClient() {
        // 初始化用户身份信息
        COSCredentials cred = new BasicCOSCredentials(cosProperties.getAccessKey(), cosProperties.getSecretKey());
        // 设置 bucket 的地域
        Region region = new Region(cosProperties.getRegion());
        ClientConfig clientConfig = new ClientConfig(region);
        // 生成 cos 客户端
        log.info("COS客户端初始化成功，region: {}, bucket: {}", cosProperties.getRegion(), cosProperties.getBucket());
        return new COSClient(cred, clientConfig);
    }

}
