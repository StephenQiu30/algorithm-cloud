package com.stephen.cloud.notification.mq;

import com.rabbitmq.client.Channel;
import com.stephen.cloud.common.rabbitmq.constants.RabbitMqConstant;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqConsumerDispatcher;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * WebSocket 消息队列消费者
 * <p>
 * 监听 RabbitMQ 消息，借助 {@link RabbitMqConsumerDispatcher} 分发单播、广播及业务通知实时下发。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class WebSocketMqConsumer {

    @Resource
    private RabbitMqConsumerDispatcher mqConsumerDispatcher;

    /**
     * 监听 WebSocket 推送队列，处理单用户或多用户推送
     *
     * @param rabbitMessage RabbitMessage 对象
     * @param channel       RabbitMQ 通道
     * @param msg           Spring AMQP 消息对象
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "", autoDelete = "true", exclusive = "true"), exchange = @Exchange(value = RabbitMqConstant.WEBSOCKET_EXCHANGE, type = "topic"), key = RabbitMqConstant.WEBSOCKET_PUSH_ROUTING_KEY), ackMode = "MANUAL")
    public void handleWebSocketPush(RabbitMessage rabbitMessage, Channel channel, Message msg) throws IOException {
        mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
    }

    /**
     * 监听 WebSocket 广播队列，将消息推送给所有在线用户
     *
     * @param rabbitMessage RabbitMessage 对象
     * @param channel       RabbitMQ 通道
     * @param msg           Spring AMQP 消息对象
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "", autoDelete = "true", exclusive = "true"), exchange = @Exchange(value = RabbitMqConstant.WEBSOCKET_EXCHANGE, type = "topic"), key = RabbitMqConstant.WEBSOCKET_BROADCAST_ROUTING_KEY), ackMode = "MANUAL")
    public void handleWebSocketBroadcast(RabbitMessage rabbitMessage, Channel channel, Message msg) throws IOException {
        mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
    }

    /**
     * 监听通知创建队列，实时推送通知给在线用户
     *
     * @param rabbitMessage RabbitMessage 对象
     * @param channel       RabbitMQ 通道
     * @param msg           Spring AMQP 消息对象
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "", autoDelete = "true", exclusive = "true"), exchange = @Exchange(value = RabbitMqConstant.NOTIFICATION_EXCHANGE, type = "topic"), key = RabbitMqConstant.NOTIFICATION_ROUTING_KEY), ackMode = "MANUAL")
    public void handleNotificationCreated(RabbitMessage rabbitMessage, Channel channel, Message msg)
            throws IOException {
        mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
    }

    /**
     * 监听 WebSocket 死信队列，打印最终失败记录
     *
     * @param rabbitMessage RabbitMessage 对象
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.WEBSOCKET_DLX_QUEUE, durable = "true"), exchange = @Exchange(value = RabbitMqConstant.WEBSOCKET_DLX_EXCHANGE, type = "topic"), key = RabbitMqConstant.WEBSOCKET_DLX_ROUTING_KEY))
    public void handleDeadLetterWebSocket(RabbitMessage rabbitMessage) {
        log.error("[WebSocketMqConsumer] WebSocket 消息进入死信队列, msgId: {}, 内容: {}",
                rabbitMessage.getMsgId(), rabbitMessage.getMsgText());
    }
}
