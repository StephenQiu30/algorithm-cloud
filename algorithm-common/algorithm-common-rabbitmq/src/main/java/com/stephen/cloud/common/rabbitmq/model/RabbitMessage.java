package com.stephen.cloud.common.rabbitmq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * RabbitMQ 消息封装
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RabbitMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息 ID
     */
    private String msgId;

    /**
     * 业务类型唯一标识
     */
    private String bizType;

    /**
     * 消息内容（JSON格式）
     */
    private String msgText;

    /**
     * 链路记录戳（供监控端审计消费者分发耗时）
     */
    @Builder.Default
    private Long timestamp = System.currentTimeMillis();
}
