package com.stephen.cloud.ai.mq;

import com.rabbitmq.client.Channel;
import com.stephen.cloud.common.rabbitmq.constants.RabbitMqConstant;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqConsumerDispatcher;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 文档分片同步指令消费者
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class ChunkSyncCommandConsumer {

    @Resource
    private RabbitMqConsumerDispatcher mqConsumerDispatcher;

    /**
     * 监听文档分片同步指令
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitMqConstant.SYNC_COMMAND_QUEUE_CHUNK, durable = "true"),
            exchange = @Exchange(value = RabbitMqConstant.SYNC_COMMAND_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = RabbitMqConstant.SYNC_COMMAND_ROUTING_KEY_CHUNK
    ), ackMode = "MANUAL")
    public void receiveSyncCommand(RabbitMessage rabbitMessage, Channel channel, Message msg) throws IOException {
        mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
    }
}
