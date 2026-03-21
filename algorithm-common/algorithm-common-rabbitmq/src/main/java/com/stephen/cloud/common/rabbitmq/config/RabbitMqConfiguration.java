package com.stephen.cloud.common.rabbitmq.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.FatalExceptionStrategy;
import org.springframework.amqp.support.converter.ContentTypeDelegatingMessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.util.ErrorHandler;

/**
 * 消息队列 RabbitMQ 实例配置
 *
 * @author StephenQiu30
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "host")
public class RabbitMqConfiguration {

    /**
     * 注册RabbitTemplate 实例
     *
     * @param connectionFactory 连接工厂接口
     * @return 返回结果
     */
    @Bean("rabbitTemplateBean")
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // 开启 Mandatory 以触发回调，确保消息进入交换机后未被队列接收时不被丢弃
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReceiveTimeout(30000);
        rabbitTemplate.setReplyTimeout(30000);

        rabbitTemplate.setMessageConverter(messageConverter);
        // 交换机和队列的回调配置
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info(">>>>>>>>>> 确认消息成功送到交换机(Exchange)，相关数据：{}", correlationData);
            } else {
                log.error(">>>>>>>>>> 确认消息没能送到交换机(Exchange)，相关数据：{}，错误原因：{}", correlationData, cause);
            }
        });

        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            log.error(">>>>>>>>>> 确认消息没能送到队列(Queue)，发生消息：{}，回应码：{}，回应信息：{}，交换机：{}，路由键值：{}",
                    returnedMessage.getMessage(),
                    returnedMessage.getReplyCode(),
                    returnedMessage.getReplyText(),
                    returnedMessage.getExchange(),
                    returnedMessage.getRoutingKey());
        });
        return rabbitTemplate;
    }

    /**
     * Jackson JSON 消息转换器（核心 Bean）
     * 用于 @RabbitListener 消费端的消息反序列化
     *
     * @return Jackson2JsonMessageConverter
     */
    @Bean
    public Jackson2JsonMessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter("*");
    }

    /**
     * 配置 RabbitTemplate 使用的消息转换器
     * 支持 application/json 和 text/plain (兼容旧数据)
     *
     * @param jacksonMessageConverter Jackson JSON 消息转换器
     * @return 消息转换器
     */
    @Bean
    public MessageConverter messageConverter(Jackson2JsonMessageConverter jacksonMessageConverter) {
        // text/plain 转换器 (兼容旧数据)
        Jackson2JsonMessageConverter textJsonConverter = new Jackson2JsonMessageConverter("*");
        textJsonConverter.setSupportedContentType(MediaType.TEXT_PLAIN);

        ContentTypeDelegatingMessageConverter converter = new ContentTypeDelegatingMessageConverter(
                jacksonMessageConverter);
        converter.addDelegate(MediaType.TEXT_PLAIN_VALUE, textJsonConverter);
        converter.addDelegate(MediaType.APPLICATION_JSON_VALUE, jacksonMessageConverter);
        return converter;
    }

    /**
     * 依赖注入日志输出
     */
    @PostConstruct
    private void initDi() {
        log.info("############ {} Configuration DI.", this.getClass().getSimpleName());
    }

    /**
     * 配置致命异常策略
     * 自动识别 MessageConversionException 等毒丸异常，避免无效重试
     */
    @Bean
    public FatalExceptionStrategy fatalExceptionStrategy() {
        return new ConditionalRejectingErrorHandler.DefaultExceptionStrategy();
    }

    /**
     * 异常处理器
     */
    @Bean
    public ErrorHandler errorHandler(FatalExceptionStrategy fatalExceptionStrategy) {
        // 使用 requireNonNull 确保元策略非空，消除 Null type safety 警告
        return new ConditionalRejectingErrorHandler(java.util.Objects.requireNonNull(fatalExceptionStrategy));
    }

    /**
     * 重试拦截器 (指数退避)
     */
    @Bean
    public RetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .backOffOptions(1000, 2.0, 10000) // 1s开始，2倍递增，上限10s
                .maxAttempts(3)
                .build();
    }

    /**
     * 配置 RabbitListenerContainerFactory
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jacksonMessageConverter,
            ErrorHandler errorHandler,
            RetryOperationsInterceptor retryInterceptor) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter);
        factory.setErrorHandler(errorHandler);
        factory.setAdviceChain(retryInterceptor);
        // prefetch 、concurrency、maxConcurrency、defaultRequeueRejected
        // 均通过 spring.rabbitmq.listener.simple.* 配置文件自动绑定，此处不再硬编码
        return factory;
    }
}
