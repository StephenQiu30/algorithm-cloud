package com.stephen.cloud.common.rabbitmq.consumer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RabbitMQ 处理器注册中心
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class RabbitMqHandlerRegistry {

    @Resource
    private ApplicationContext applicationContext;

    private final Map<String, RabbitMqHandler<?>> handlerMap = new ConcurrentHashMap<>();

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() {
        Map<String, RabbitMqHandler<?>> handlers = (Map<String, RabbitMqHandler<?>>) (Map<?, ?>) applicationContext
                .getBeansOfType(RabbitMqHandler.class);
        handlers.values().forEach(handler -> {
            String bizType = handler.getBizType();
            if (handlerMap.containsKey(bizType)) {
                log.error("[RabbitMqHandlerRegistry] 发现冲突的 RabbitMQ Handler bizType: {}, 已有: {}, 新发现: {}",
                        bizType, handlerMap.get(bizType).getClass().getSimpleName(),
                        handler.getClass().getSimpleName());
                throw new IllegalStateException("Duplicate RabbitMqHandler bizType: " + bizType);
            }
            handlerMap.put(bizType, handler);
            log.info("[RabbitMqHandlerRegistry] 注册 Handler: [bizType = {}] -> [class = {}]",
                    bizType, handler.getClass().getSimpleName());
        });
    }

    @SuppressWarnings("unchecked")
    public <T> RabbitMqHandler<T> getHandler(String bizType) {
        return (RabbitMqHandler<T>) handlerMap.get(bizType);
    }
}

