package com.stephen.cloud.common.rabbitmq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 消费者配置属性
 * <p>
 * 映射 {@code mq.consumer.*} 命名空间下的配置项。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Component
@ConfigurationProperties(prefix = "mq.consumer")
public class RabbitMqConsumerProperties {

    /**
     * 单条消息消费耗时告警阈值（毫秒）
     */
    private long slowConsumeThresholdMs = 5000L;
}

