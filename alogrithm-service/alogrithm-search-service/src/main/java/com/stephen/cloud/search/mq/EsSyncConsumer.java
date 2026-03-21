package com.stephen.cloud.search.mq;

import com.stephen.cloud.common.rabbitmq.constants.RabbitMqConstant;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqConsumerDispatcher;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class EsSyncConsumer {

    @Resource
    private RabbitMqConsumerDispatcher mqConsumerDispatcher;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.ES_SYNC_QUEUE, durable = "true", arguments = {
            @Argument(name = "x-dead-letter-exchange", value = RabbitMqConstant.ES_SYNC_DLX_EXCHANGE),
            @Argument(name = "x-dead-letter-routing-key", value = RabbitMqConstant.ES_SYNC_DLX_ROUTING_KEY)
    }), exchange = @Exchange(value = RabbitMqConstant.ES_SYNC_EXCHANGE, type = ExchangeTypes.DIRECT), key = RabbitMqConstant.ES_SYNC_ROUTING_KEY), ackMode = "MANUAL")
    public void receiveEsSyncMessage(RabbitMessage rabbitMessage, com.rabbitmq.client.Channel channel, Message msg) throws IOException {
        mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.ES_SYNC_DLX_QUEUE, durable = "true"), exchange = @Exchange(value = RabbitMqConstant.ES_SYNC_DLX_EXCHANGE, type = "topic"), key = RabbitMqConstant.ES_SYNC_DLX_ROUTING_KEY))
    public void handleDeadLetterEsSync(RabbitMessage rabbitMessage) {
        if (rabbitMessage == null) {
            return;
        }
        log.error("[EsSyncConsumer] 消息进入死信队列，请人工介入检查: [msgId={}], [content={}]",
                rabbitMessage.getMsgId(), rabbitMessage.getMsgText());
    }
}
