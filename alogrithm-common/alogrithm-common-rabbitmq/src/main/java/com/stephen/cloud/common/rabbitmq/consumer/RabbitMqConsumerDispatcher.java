package com.stephen.cloud.common.rabbitmq.consumer;

import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.stephen.cloud.common.cache.constants.KeyPrefixConstants;
import com.stephen.cloud.common.rabbitmq.config.RabbitMqConsumerProperties;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RabbitMQ 消费端核心分发器
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class RabbitMqConsumerDispatcher {

    @Resource
    private RabbitMqHandlerRegistry handlerRegistry;

    @Resource
    private RabbitMqDedupeStore dedupeStore;

    @Resource
    private RabbitMqConsumerProperties consumerProperties;

    public <T> void dispatch(RabbitMessage rabbitMessage, Channel channel, Message msg) throws IOException {
        long deliveryTag = msg.getMessageProperties().getDeliveryTag();
        long startTime = System.currentTimeMillis();

        if (rabbitMessage == null || rabbitMessage.getBizType() == null || rabbitMessage.getMsgId() == null) {
            log.error("[RabbitMqConsumerDispatcher] 关键信息缺失，拒绝消费: {}", rabbitMessage);
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizType = rabbitMessage.getBizType();
        String msgId = rabbitMessage.getMsgId();

        RabbitMqHandler<T> handler = handlerRegistry.getHandler(bizType);
        if (handler == null) {
            log.warn("[RabbitMqConsumerDispatcher] 未匹配到处理器: [bizType={}], [msgId={}]，已 ACK 防止堆积", bizType, msgId);
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            RabbitMqDedupeLock annotation = handler.getClass().getAnnotation(RabbitMqDedupeLock.class);
            if (annotation != null) {
                String dedupeKey = annotation.prefix() + ":" + bizType + ":" + msgId;
                String fullKey = KeyPrefixConstants.IDEMPOTENT_PREFIX + dedupeKey;
                if (!dedupeStore.putIfAbsent(fullKey, annotation.expire())) {
                    log.info("[RabbitMqConsumerDispatcher] 触发去重拦截，跳过重复处理: [bizType={}], [msgId={}]", bizType, msgId);
                    channel.basicAck(deliveryTag, false);
                    return;
                }
            }

            T data = JSONUtil.toBean(rabbitMessage.getMsgText(), handler.getDataType());
            if (data == null) {
                log.error("[RabbitMqConsumerDispatcher] 反序列化失败: [bizType={}], [msgId={}]", bizType, msgId);
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            handler.onMessage(data, rabbitMessage);

            channel.basicAck(deliveryTag, false);

            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > consumerProperties.getSlowConsumeThresholdMs()) {
                log.warn("[RabbitMqConsumerDispatcher] 消费耗时过长: [bizType={}], [msgId={}], [耗时={}ms]", bizType, msgId, elapsed);
            } else {
                log.info("[RabbitMqConsumerDispatcher] 消费成功: [bizType={}], [msgId={}], [耗时={}ms]", bizType, msgId, elapsed);
            }
        } catch (Exception e) {
            log.error("[RabbitMqConsumerDispatcher] 业务执行异常: [bizType={}], [msgId={}]", bizType, msgId, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}

