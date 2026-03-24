package com.stephen.cloud.ai.mq;

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

@Slf4j
@Component
public class DocumentProcessMqConsumer {

    private static final String KNOWLEDGE_DOC_INGEST_DLX_EXCHANGE = "algorithm.knowledge.doc.ingest.dlx.exchange";
    private static final String KNOWLEDGE_DOC_INGEST_DLX_QUEUE = "algorithm.knowledge.doc.ingest.dlx.queue";
    private static final String KNOWLEDGE_DOC_INGEST_DLX_ROUTING_KEY = "algorithm.knowledge.doc.ingest.dlx";

    @Resource
    private RabbitMqConsumerDispatcher mqConsumerDispatcher;

    /**
     * 监听知识文档入库队列，并由统一分发器下发给具体业务 Handler。
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.KNOWLEDGE_DOC_INGEST_QUEUE, durable = "true", arguments = {
            @Argument(name = "x-dead-letter-exchange", value = KNOWLEDGE_DOC_INGEST_DLX_EXCHANGE),
            @Argument(name = "x-dead-letter-routing-key", value = KNOWLEDGE_DOC_INGEST_DLX_ROUTING_KEY)
    }), exchange = @Exchange(value = RabbitMqConstant.KNOWLEDGE_DOC_INGEST_EXCHANGE, type = "direct"), key = RabbitMqConstant.KNOWLEDGE_DOC_INGEST_ROUTING_KEY), ackMode = "MANUAL")
    public void handleKnowledgeDocIngest(RabbitMessage rabbitMessage, Channel channel, Message msg) throws IOException {
        mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
    }

    /**
     * 监听知识文档处理死信队列，记录最终失败消息以便排障。
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = KNOWLEDGE_DOC_INGEST_DLX_QUEUE, durable = "true"),
            exchange = @Exchange(value = KNOWLEDGE_DOC_INGEST_DLX_EXCHANGE, type = "topic"),
            key = KNOWLEDGE_DOC_INGEST_DLX_ROUTING_KEY))
    public void handleDeadLetterKnowledgeDocIngest(RabbitMessage rabbitMessage) {
        log.error("[DocumentProcessMqConsumer] 文档处理消息进入死信队列, msgId: {}, 内容: {}",
                rabbitMessage.getMsgId(), rabbitMessage.getMsgText());
    }
}
