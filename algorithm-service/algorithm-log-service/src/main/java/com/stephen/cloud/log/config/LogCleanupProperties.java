package com.stephen.cloud.log.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "log.cleanup")
public class LogCleanupProperties {

    private boolean enabled = false;

    private int operationRetentionDays = 30;

    private int apiAccessRetentionDays = 14;

    private int loginRetentionDays = 30;
}

