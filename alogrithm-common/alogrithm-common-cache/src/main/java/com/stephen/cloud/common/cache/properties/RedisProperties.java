package com.stephen.cloud.common.cache.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis 配置属性
 * 统一管理 Redis 连接配置
 *
 * @author StephenQiu30
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedisProperties {

    /**
     * Redis 服务器地址
     */
    private String host = "localhost";

    /**
     * Redis 服务器端口
     */
    private int port = 6379;

    /**
     * Redis 数据库索引
     */
    private int database = 0;

    /**
     * Redis 密码
     */
    private String password;

    /**
     * 连接超时时间（毫秒）
     */
    private int timeout = 5000;
}
