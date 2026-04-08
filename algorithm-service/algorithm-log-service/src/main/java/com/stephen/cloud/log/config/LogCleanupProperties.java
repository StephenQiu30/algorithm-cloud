package com.stephen.cloud.log.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 日志清理配置属性
 * <p>
 * 控制各类日志的保留天数，支持定时清理任务
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Component
@ConfigurationProperties(prefix = "log.cleanup")
public class LogCleanupProperties {

    private boolean enabled = false;

    private int operationRetentionDays = 30;

    private int apiAccessRetentionDays = 14;

    private int loginRetentionDays = 30;
}

