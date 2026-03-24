package com.stephen.cloud.ai.config;

import com.stephen.cloud.common.rabbitmq.constants.RabbitMqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue documentProcessQueue() {
        return new Queue(RabbitMqConstant.KNOWLEDGE_DOC_INGEST_QUEUE, true);
    }

    @Bean
    public DirectExchange documentProcessExchange() {
        return new DirectExchange(RabbitMqConstant.KNOWLEDGE_DOC_INGEST_EXCHANGE, true, false);
    }

    @Bean
    public Binding documentProcessBinding(Queue documentProcessQueue, DirectExchange documentProcessExchange) {
        return BindingBuilder.bind(documentProcessQueue)
                .to(documentProcessExchange)
                .with(RabbitMqConstant.KNOWLEDGE_DOC_INGEST_ROUTING_KEY);
    }
}
