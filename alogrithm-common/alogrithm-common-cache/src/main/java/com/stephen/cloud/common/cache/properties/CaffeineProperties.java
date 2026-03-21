package com.stephen.cloud.common.cache.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Caffeine 本地缓存配置属性
 *
 * @author StephenQiu30
 */
@Data
@Component
@ConfigurationProperties(prefix = "caffeine")
public class CaffeineProperties {

    private Long expired = 300L;

    private Integer initCapacity = 100;

    private Integer maxCapacity = 10000;

    private Boolean enabled = true;
}
