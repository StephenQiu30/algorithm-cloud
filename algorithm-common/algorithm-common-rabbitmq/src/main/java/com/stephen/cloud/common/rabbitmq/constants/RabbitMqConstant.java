package com.stephen.cloud.common.rabbitmq.constants;

/**
 * RabbitMQ 常量
 *
 * @author StephenQiu30
 */
public interface RabbitMqConstant {

    // ==================== 邮件相关 ====================

    /**
     * 邮件交换机
     */
    String EMAIL_EXCHANGE = "algorithm.email.exchange";

    /**
     * 邮件队列
     */
    String EMAIL_QUEUE = "algorithm.email.queue";

    /**
     * 邮件路由键
     */
    String EMAIL_ROUTING_KEY = "algorithm.email.send";

    /**
     * 邮件死信交换机
     */
    String EMAIL_DLX_EXCHANGE = "algorithm.email.dlx.exchange";

    /**
     * 邮件死信队列
     */
    String EMAIL_DLX_QUEUE = "algorithm.email.dlx.queue";

    /**
     * 邮件死信路由键
     */
    String EMAIL_DLX_ROUTING_KEY = "algorithm.email.dlx";

    // ==================== WebSocket 相关 ====================

    /**
     * WebSocket 交换机
     */
    String WEBSOCKET_EXCHANGE = "algorithm.websocket.exchange";

    /**
     * WebSocket 推送队列
     */
    String WEBSOCKET_PUSH_QUEUE = "algorithm.websocket.push.queue";

    /**
     * WebSocket 推送路由键
     */
    String WEBSOCKET_PUSH_ROUTING_KEY = "algorithm.websocket.push";

    /**
     * WebSocket 广播队列
     */
    String WEBSOCKET_BROADCAST_QUEUE = "algorithm.websocket.broadcast.queue";

    /**
     * WebSocket 广播路由键
     */
    String WEBSOCKET_BROADCAST_ROUTING_KEY = "algorithm.websocket.broadcast";

    /**
     * WebSocket 死信交换机
     */
    String WEBSOCKET_DLX_EXCHANGE = "algorithm.websocket.dlx.exchange";

    /**
     * WebSocket 死信队列
     */
    String WEBSOCKET_DLX_QUEUE = "algorithm.websocket.dlx.queue";

    /**
     * WebSocket 死信路由键
     */
    String WEBSOCKET_DLX_ROUTING_KEY = "algorithm.websocket.dlx";

    // ==================== Elasticsearch 同步相关 ====================

    /**
     * ES 同步交换机
     */
    String ES_SYNC_EXCHANGE = "algorithm.es.sync.exchange";

    /**
     * ES 同步队列
     */
    String ES_SYNC_QUEUE = "algorithm.es.sync.queue";

    /**
     * ES 同步路由键
     */
    String ES_SYNC_ROUTING_KEY = "algorithm.es.sync";

    /**
     * ES 同步死信交换机
     */
    String ES_SYNC_DLX_EXCHANGE = "algorithm.es.sync.dlx.exchange";

    /**
     * ES 同步死信队列
     */
    String ES_SYNC_DLX_QUEUE = "algorithm.es.sync.dlx.queue";

    /**
     * ES 同步死信路由键
     */
    String ES_SYNC_DLX_ROUTING_KEY = "algorithm.es.sync.dlx";

    // ==================== Notification 相关 ====================

    /**
     * 通知交换机
     */
    String NOTIFICATION_EXCHANGE = "algorithm.notification.exchange";

    /**
     * 通知队列
     */
    String NOTIFICATION_QUEUE = "algorithm.notification.queue";

    /**
     * 通知路由键
     */
    String NOTIFICATION_ROUTING_KEY = "algorithm.notification.create";

    /**
     * 通知死信交换机
     */
    String NOTIFICATION_DLX_EXCHANGE = "algorithm.notification.dlx.exchange";

    /**
     * 通知死信队列
     */
    String NOTIFICATION_DLX_QUEUE = "algorithm.notification.dlx.queue";

    /**
     * 通知死信路由键
     */
    String NOTIFICATION_DLX_ROUTING_KEY = "algorithm.notification.dlx";

    /**
     * 评论事件队列
     */
    String COMMENT_EVENT_QUEUE = "algorithm.notification.comment.queue";

    /**
     * 评论事件路由键
     */
    String COMMENT_EVENT_ROUTING_KEY = "algorithm.event.comment.create";

    /**
     * 点赞事件队列
     */
    String LIKE_EVENT_QUEUE = "algorithm.notification.like.queue";

    /**
     * 点赞事件路由键
     */
    String LIKE_EVENT_ROUTING_KEY = "algorithm.event.like.create";

    /**
     * 收藏事件队列
     */
    String FAVOUR_EVENT_QUEUE = "algorithm.notification.favour.queue";

    /**
     * 收藏事件路由键
     */
    String FAVOUR_EVENT_ROUTING_KEY = "algorithm.event.favour.create";

    /**
     * 关注事件队列
     */
    String FOLLOW_EVENT_QUEUE = "algorithm.notification.follow.queue";

    /**
     * 关注事件路由键
     */
    String FOLLOW_EVENT_ROUTING_KEY = "algorithm.event.follow.create";

    /**
     * 帖子审核事件队列
     */
    String POST_REVIEW_EVENT_QUEUE = "algorithm.notification.post_review.queue";

    /**
     * 帖子审核事件路由键
     */
    String POST_REVIEW_EVENT_ROUTING_KEY = "algorithm.event.post_review.result";

    // ==================== 数据同步指令相关 ====================

    /**
     * 数据同步指令交换机 (Topic)
     */
    String SYNC_COMMAND_EXCHANGE = "algorithm.sync.command.exchange";

    /**
     * 帖子数据同步指令队列
     */
    String SYNC_COMMAND_QUEUE_POST = "algorithm.sync.command.post.queue";

    /**
     * 用户数据同步指令队列
     */
    String SYNC_COMMAND_QUEUE_USER = "algorithm.sync.command.user.queue";

    /**
     * 帖子数据同步指令路由键
     */
    String SYNC_COMMAND_ROUTING_KEY_POST = "algorithm.sync.command.post";

    /**
     * 用户数据同步指令路由键
     */
    String SYNC_COMMAND_ROUTING_KEY_USER = "algorithm.sync.command.user";

    // ==================== AI 相关 ====================

    /**
     * AI 对话记录交换机
     */
    String AI_CHAT_RECORD_EXCHANGE = "algorithm.ai.chat.record.exchange";

    /**
     * AI 对话记录队列
     */
    String AI_CHAT_RECORD_QUEUE = "algorithm.ai.chat.record.queue";

    /**
     * AI 对话记录路由键
     */
    String AI_CHAT_RECORD_ROUTING_KEY = "algorithm.ai.chat.record.create";

    String KNOWLEDGE_DOC_INGEST_EXCHANGE = "algorithm.knowledge.doc.ingest.exchange";

    String KNOWLEDGE_DOC_INGEST_QUEUE = "algorithm.knowledge.doc.ingest.queue";

    String KNOWLEDGE_DOC_INGEST_ROUTING_KEY = "algorithm.knowledge.doc.ingest";
}
