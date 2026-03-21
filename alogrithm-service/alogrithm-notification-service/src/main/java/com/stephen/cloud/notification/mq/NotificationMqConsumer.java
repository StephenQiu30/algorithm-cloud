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
 * 通知消息队列消费者
 * <p>
 * 监听业务事件队列（评论、点赞、关注），采用 {@link RabbitMqConsumerDispatcher} 进行标准化分发处理，
 * 自动创建对应类型的通知。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class NotificationMqConsumer {

    @Resource
    private RabbitMqConsumerDispatcher mqConsumerDispatcher;

    /**
     * 监听评论事件队列，自动创建评论通知
     *
     * @param rabbitMessage RabbitMessage 对象
     * @param channel       RabbitMQ 通道
     * @param msg           Spring AMQP 消息对象
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.COMMENT_EVENT_QUEUE, durable = "true", arguments = {
            @Argument(name = "x-dead-letter-exchange", value = RabbitMqConstant.NOTIFICATION_DLX_EXCHANGE),
            @Argument(name = "x-dead-letter-routing-key", value = RabbitMqConstant.NOTIFICATION_DLX_ROUTING_KEY)
    }), exchange = @Exchange(value = RabbitMqConstant.NOTIFICATION_EXCHANGE, type = "topic"), key = RabbitMqConstant.COMMENT_EVENT_ROUTING_KEY), ackMode = "MANUAL")
    public void handleCommentEvent(RabbitMessage rabbitMessage, Channel channel, Message msg) throws IOException {
        mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
    }

    /**
     * 监听点赞事件队列，自动创建点赞通知
     *
     * @param rabbitMessage RabbitMessage 对象
     * @param channel       RabbitMQ 通道
     * @param msg           Spring AMQP 消息对象
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.LIKE_EVENT_QUEUE, durable = "true", arguments = {
            @Argument(name = "x-dead-letter-exchange", value = RabbitMqConstant.NOTIFICATION_DLX_EXCHANGE),
            @Argument(name = "x-dead-letter-routing-key", value = RabbitMqConstant.NOTIFICATION_DLX_ROUTING_KEY)
    }), exchange = @Exchange(value = RabbitMqConstant.NOTIFICATION_EXCHANGE, type = "topic"), key = RabbitMqConstant.LIKE_EVENT_ROUTING_KEY), ackMode = "MANUAL")
    public void handleLikeEvent(RabbitMessage rabbitMessage, Channel channel, Message msg) throws IOException {
        mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
    }

    /**
     * 监听收藏事件队列，自动创建收藏通知
     *
     * @param rabbitMessage RabbitMessage 对象
     * @param channel       RabbitMQ 通道
     * @param msg           Spring AMQP 消息对象
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.FAVOUR_EVENT_QUEUE, durable = "true", arguments = {
            @Argument(name = "x-dead-letter-exchange", value = RabbitMqConstant.NOTIFICATION_DLX_EXCHANGE),
            @Argument(name = "x-dead-letter-routing-key", value = RabbitMqConstant.NOTIFICATION_DLX_ROUTING_KEY)
    }), exchange = @Exchange(value = RabbitMqConstant.NOTIFICATION_EXCHANGE, type = "topic"), key = RabbitMqConstant.FAVOUR_EVENT_ROUTING_KEY), ackMode = "MANUAL")
    public void handleFavourEvent(RabbitMessage rabbitMessage, Channel channel, Message msg) throws IOException {
        mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
    }

    /**
     * 监听关注事件队列，自动创建关注通知
     *
     * @param rabbitMessage RabbitMessage 对象
     * @param channel       RabbitMQ 通道
     * @param msg           Spring AMQP 消息对象
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.FOLLOW_EVENT_QUEUE, durable = "true", arguments = {
            @Argument(name = "x-dead-letter-exchange", value = RabbitMqConstant.NOTIFICATION_DLX_EXCHANGE),
            @Argument(name = "x-dead-letter-routing-key", value = RabbitMqConstant.NOTIFICATION_DLX_ROUTING_KEY)
    }), exchange = @Exchange(value = RabbitMqConstant.NOTIFICATION_EXCHANGE, type = "topic"), key = RabbitMqConstant.FOLLOW_EVENT_ROUTING_KEY), ackMode = "MANUAL")
    public void handleFollowEvent(RabbitMessage rabbitMessage, Channel channel, Message msg) throws IOException {
        mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
    }

    /**
     * 监听帖子审核结果事件队列，自动创建审核通知
     *
     * @param rabbitMessage RabbitMessage 对象
     * @param channel       RabbitMQ 通道
     * @param msg           Spring AMQP 消息对象
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.POST_REVIEW_EVENT_QUEUE, durable = "true", arguments = {
            @Argument(name = "x-dead-letter-exchange", value = RabbitMqConstant.NOTIFICATION_DLX_EXCHANGE),
            @Argument(name = "x-dead-letter-routing-key", value = RabbitMqConstant.NOTIFICATION_DLX_ROUTING_KEY)
    }), exchange = @Exchange(value = RabbitMqConstant.NOTIFICATION_EXCHANGE, type = "topic"), key = RabbitMqConstant.POST_REVIEW_EVENT_ROUTING_KEY), ackMode = "MANUAL")
    public void handlePostReviewEvent(RabbitMessage rabbitMessage, Channel channel, Message msg) throws IOException {
        mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
    }

    /**
     * 监听通知系统死信队列，打印最终失败记录
     *
     * @param rabbitMessage RabbitMessage 对象
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = RabbitMqConstant.NOTIFICATION_DLX_QUEUE, durable = "true"), exchange = @Exchange(value = RabbitMqConstant.NOTIFICATION_DLX_EXCHANGE, type = "topic"), key = RabbitMqConstant.NOTIFICATION_DLX_ROUTING_KEY))
    public void handleDeadLetterNotification(RabbitMessage rabbitMessage) {
        log.error("[NotificationMqConsumer] 通知消息进入死信队列, msgId: {}, 内容: {}",
                rabbitMessage.getMsgId(), rabbitMessage.getMsgText());
    }
}
