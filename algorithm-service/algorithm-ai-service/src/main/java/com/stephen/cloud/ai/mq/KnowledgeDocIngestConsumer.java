package com.stephen.cloud.ai.mq;

import com.rabbitmq.client.Channel;
import com.stephen.cloud.common.rabbitmq.constants.RabbitMqConstant;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqConsumerDispatcher;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class KnowledgeDocIngestConsumer {

    @Resource
    private RabbitMqConsumerDispatcher mqConsumerDispatcher;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitMqConstant.KNOWLEDGE_DOC_INGEST_QUEUE, durable = "true"),
            exchange = @Exchange(value = RabbitMqConstant.KNOWLEDGE_DOC_INGEST_EXCHANGE, type = "direct"),
            key = RabbitMqConstant.KNOWLEDGE_DOC_INGEST_ROUTING_KEY
    ), ackMode = "MANUAL")
    public void handleKnowledgeDocIngest(RabbitMessage rabbitMessage, Channel channel, Message msg) throws IOException {
        mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
    }
}
