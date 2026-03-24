package com.stephen.cloud.common.rabbitmq.enums;

import com.stephen.cloud.common.rabbitmq.constants.RabbitMqConstant;
import lombok.Getter;

/**
 * MQ 业务类型策略枚举
 * <p>
 * 采用策略与枚举结合的设计模式，集中管理所有的 Exchange, RoutingKey 和 BizType。
 * 消除散落于各处的硬编码，使得发送时具备强类型检查。
 * </p>
 *
 * @author StephenQiu30
 */
@Getter
public enum MqBizTypeEnum {

    /**
     * Elasticsearch 同步 - 单条记录更新/同步
     */
    ES_SYNC_SINGLE("ES_SYNC_SINGLE", RabbitMqConstant.ES_SYNC_EXCHANGE, RabbitMqConstant.ES_SYNC_ROUTING_KEY),

    /**
     * Elasticsearch 同步 - 批量更新/同步
     */
    ES_SYNC_BATCH("ES_SYNC_BATCH", RabbitMqConstant.ES_SYNC_EXCHANGE, RabbitMqConstant.ES_SYNC_ROUTING_KEY),

    /**
     * WebSocket 推送 - 单发或组发
     */
    WEBSOCKET_PUSH("WEBSOCKET_PUSH", RabbitMqConstant.WEBSOCKET_EXCHANGE, RabbitMqConstant.WEBSOCKET_PUSH_ROUTING_KEY),

    /**
     * WebSocket 推送 - 全服广播
     */
    WEBSOCKET_BROADCAST("WEBSOCKET_BROADCAST", RabbitMqConstant.WEBSOCKET_EXCHANGE,
            RabbitMqConstant.WEBSOCKET_BROADCAST_ROUTING_KEY),

    /**
     * 邮件发送配置
     */
    EMAIL_SEND("EMAIL_SEND", RabbitMqConstant.EMAIL_EXCHANGE, RabbitMqConstant.EMAIL_ROUTING_KEY),

    /**
     * 全局通知
     */
    NOTIFICATION_SEND("NOTIFICATION_SEND", RabbitMqConstant.NOTIFICATION_EXCHANGE,
            RabbitMqConstant.NOTIFICATION_ROUTING_KEY),

    /**
     * 评论事件
     */
    COMMENT_EVENT("COMMENT_EVENT", RabbitMqConstant.NOTIFICATION_EXCHANGE,
            RabbitMqConstant.COMMENT_EVENT_ROUTING_KEY),

    /**
     * 点赞事件
     */
    LIKE_EVENT("LIKE_EVENT", RabbitMqConstant.NOTIFICATION_EXCHANGE,
            RabbitMqConstant.LIKE_EVENT_ROUTING_KEY),

    /**
     * 收藏事件
     */
    FAVOUR_EVENT("FAVOUR_EVENT", RabbitMqConstant.NOTIFICATION_EXCHANGE,
            RabbitMqConstant.FAVOUR_EVENT_ROUTING_KEY),

    /**
     * 关注事件
     */
    FOLLOW_EVENT("FOLLOW_EVENT", RabbitMqConstant.NOTIFICATION_EXCHANGE,
            RabbitMqConstant.FOLLOW_EVENT_ROUTING_KEY),

    /**
     * 帖子审核结果事件
     */
    POST_REVIEW_EVENT("POST_REVIEW_EVENT", RabbitMqConstant.NOTIFICATION_EXCHANGE,
            RabbitMqConstant.POST_REVIEW_EVENT_ROUTING_KEY),

    /**
     * 帖子数据同步指令
     */
    SYNC_COMMAND_POST("SYNC_COMMAND_POST", RabbitMqConstant.SYNC_COMMAND_EXCHANGE,
            RabbitMqConstant.SYNC_COMMAND_ROUTING_KEY_POST),

    /**
     * 用户数据同步指令
     */
    SYNC_COMMAND_USER("SYNC_COMMAND_USER", RabbitMqConstant.SYNC_COMMAND_EXCHANGE,
            RabbitMqConstant.SYNC_COMMAND_ROUTING_KEY_USER),

    /**
     * 文档分片数据同步指令
     */
    SYNC_COMMAND_CHUNK("SYNC_COMMAND_CHUNK", RabbitMqConstant.SYNC_COMMAND_EXCHANGE,
            RabbitMqConstant.SYNC_COMMAND_ROUTING_KEY_CHUNK),

    /**
     * AI 对话记录同步
     */
    AI_CHAT_RECORD("AI_CHAT_RECORD", RabbitMqConstant.AI_CHAT_RECORD_EXCHANGE,
            RabbitMqConstant.AI_CHAT_RECORD_ROUTING_KEY),

    KNOWLEDGE_DOC_INGEST("KNOWLEDGE_DOC_INGEST", RabbitMqConstant.KNOWLEDGE_DOC_INGEST_EXCHANGE,
            RabbitMqConstant.KNOWLEDGE_DOC_INGEST_ROUTING_KEY);

    /**
     * 业务类型唯一标识，由 Listener/Handler 使用 @MqHandler(bizType = "...") 进行订阅匹配
     */
    private final String value;

    /**
     * 目标交换机名称
     */
    private final String exchange;

    /**
     * 目标路由键
     */
    private final String routingKey;

    MqBizTypeEnum(String value, String exchange, String routingKey) {
        this.value = value;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }
}
