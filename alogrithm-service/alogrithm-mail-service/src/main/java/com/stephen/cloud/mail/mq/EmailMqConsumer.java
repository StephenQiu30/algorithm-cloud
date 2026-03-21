package com.stephen.cloud.mail.mq;

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
 * 邮件消息队列消费者
 * <p>
 * 监听邮件队列，采用 {@link RabbitMqConsumerDispatcher} 实现标准化分发。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class EmailMqConsumer {

    @Resource
    private RabbitMqConsumerDispatcher mqConsumerDispatcher;

    /**
     * 监听邮件队列，消费邮件发送消息
     *
     * @param rabbitMessage RabbitMessage 对象
     * @param channel       RabbitMQ 通道
     * @param msg           Spring AMQP 消息对象
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.EMAIL_QUEUE, durable = "true", arguments = {
            @Argument(name = "x-dead-letter-exchange", value = RabbitMqConstant.EMAIL_DLX_EXCHANGE),
            @Argument(name = "x-dead-letter-routing-key", value = RabbitMqConstant.EMAIL_DLX_ROUTING_KEY)
    }), exchange = @Exchange(value = RabbitMqConstant.EMAIL_EXCHANGE, type = "topic"), key = RabbitMqConstant.EMAIL_ROUTING_KEY), ackMode = "MANUAL")
    public void handleEmailMessage(RabbitMessage rabbitMessage, Channel channel, Message msg) throws IOException {
        mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
    }

    /**
     * 监听邮件死信队列，打印最终失败记录
     *
     * @param rabbitMessage RabbitMessage 对象
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.EMAIL_DLX_QUEUE, durable = "true"), exchange = @Exchange(value = RabbitMqConstant.EMAIL_DLX_EXCHANGE, type = "topic"), key = RabbitMqConstant.EMAIL_DLX_ROUTING_KEY))
    public void handleDeadLetterEmail(RabbitMessage rabbitMessage) {
        log.error("[EmailMqConsumer] 邮件进入死信队列, msgId: {}, 内容: {}",
                rabbitMessage.getMsgId(), rabbitMessage.getMsgText());
    }
}
