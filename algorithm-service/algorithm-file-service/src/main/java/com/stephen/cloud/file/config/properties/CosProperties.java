package com.stephen.cloud.file.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 腾讯云 COS 配置属性
 *
 * @author StephenQiu30
 */
@Data
@ConfigurationProperties(prefix = "cos.client")
public class CosProperties {

    /**
     * 是否启用
     */
    private Boolean enable = false;

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 密钥
     */
    private String secretKey;

    /**
     * 地域
     */
    private String region;

    /**
     * 桶名称
     */
    private String bucket;
}
