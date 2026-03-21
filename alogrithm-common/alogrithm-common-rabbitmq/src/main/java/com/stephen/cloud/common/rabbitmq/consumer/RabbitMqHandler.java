package com.stephen.cloud.common.rabbitmq.consumer;

import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;

/**
 * RabbitMQ 业务处理器核心接口
 * <p>
 * 每种业务类型（BizType）对应一个具体处理器实现，由分发器统一调度。
 * </p>
 *
 * @param <T> 消息体解包后的泛型类型
 * @author StephenQiu30
 */
public interface RabbitMqHandler<T> {

    /**
     * 获取当前处理器对应的业务类型（BizType）。
     */
    String getBizType();

    /**
     * 处理核心业务逻辑。
     */
    void onMessage(T data, RabbitMessage rabbitMessage) throws Exception;

    /**
     * 获取业务数据的实际类型，用于反序列化。
     */
    Class<T> getDataType();
}

