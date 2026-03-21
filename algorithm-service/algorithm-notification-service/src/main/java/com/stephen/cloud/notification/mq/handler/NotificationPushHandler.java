package com.stephen.cloud.notification.mq.handler;

import cn.hutool.json.JSONUtil;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketMessageTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketPushTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.NotificationMessage;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 通知系统实时推送处理器
 * <p>
 * 负责消费 NOTIFICATION_SEND 类型的消息。
 * 主要逻辑：将通知消息重新封装为 WebSocket 协议格式，并根据用户 ID 决定单播或广播。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class NotificationPushHandler implements RabbitMqHandler<NotificationMessage> {

    @Resource
    private ChannelManager channelManager;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.NOTIFICATION_SEND.getValue();
    }

    @Override
    public void onMessage(NotificationMessage notificationMessage, RabbitMessage rabbitMessage) throws Exception {
        String msgId = rabbitMessage.getMsgId();

        if (notificationMessage.getUserId() == null) {
            log.error("[NotificationPushHandler] 通知推送消息解析失败或缺少用户ID, msgId: {}", msgId);
            throw new IllegalArgumentException("缺少用户ID");
        }

        Long userId = notificationMessage.getUserId();
        log.info("[NotificationPushHandler] 收到通知实时推送事件, userId: {}, msgId: {}", userId, msgId);

        // 构造统一的消息格式
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .userId(userId)
                .type(WebSocketMessageTypeEnum.SYSTEM_NOTICE.getCode())
                .data(notificationMessage)
                .bizId(msgId)
                .pushType(userId == 0L ? WebSocketPushTypeEnum.BROADCAST.getValue()
                        : WebSocketPushTypeEnum.SINGLE.getValue())
                .build();

        String messageJson = JSONUtil.toJsonStr(wsMessage);

        if (userId == 0L) {
            // 广播推送
            channelManager.getAllChannels().writeAndFlush(new TextWebSocketFrame(messageJson));
            log.info("[NotificationPushHandler] 成功广播通知消息给所有在线用户, msgId: {}", msgId);
        } else {
            // 单播推送
            pushToSingleUser(userId, wsMessage);
        }
    }

    @Override
    public Class<NotificationMessage> getDataType() {
        return NotificationMessage.class;
    }

    private void pushToSingleUser(Long userId, WebSocketMessage wsMessage) {
        String userIdStr = String.valueOf(userId);

        if (!channelManager.isOnline(userIdStr)) {
            log.debug("[NotificationPushHandler] 用户 {} 不在本服务器实例, 忽略推送", userId);
            return;
        }

        io.netty.channel.Channel channel = channelManager.getChannel(userIdStr);
        if (channel == null || !channel.isActive()) {
            log.warn("[NotificationPushHandler] 用户 {} 的 WebSocket 连接不可用", userId);
            return;
        }

        channel.writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(wsMessage)));
        log.info("[NotificationPushHandler] 成功向用户 {} 推送实时通知消息", userId);
    }
}
