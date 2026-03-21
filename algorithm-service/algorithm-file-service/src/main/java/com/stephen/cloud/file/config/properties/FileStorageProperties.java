package com.stephen.cloud.file.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件存储配置属性
 *
 * @author StephenQiu30
 */
@Data
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    /**
     * 存储类型
     */
    private String type = "COS";

    /**
     * 路径前缀
     */
    private String pathPrefix = "algorithm";

}
