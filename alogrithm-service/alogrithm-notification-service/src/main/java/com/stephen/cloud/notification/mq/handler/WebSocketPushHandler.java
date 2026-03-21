package com.stephen.cloud.notification.mq.handler;

import cn.hutool.json.JSONUtil;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WebSocket 通用推送处理器
 * <p>
 * 负责消费 WEBSOCKET_PUSH 类型的消息。
 * 支持多种推送模式：
 * 1. <b>单用户模式</b>：指定单个 userId 时的点对点推送。
 * 2. <b>多用户模式</b>：指定 userIds 集合时的本地实例在线用户组播。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class WebSocketPushHandler implements RabbitMqHandler<WebSocketMessage> {

    @Resource
    private ChannelManager channelManager;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.WEBSOCKET_PUSH.getValue();
    }

    @Override
    public void onMessage(WebSocketMessage wsMessage, RabbitMessage rabbitMessage) throws Exception {
        String msgId = rabbitMessage.getMsgId();

        log.info("[WebSocketPushHandler] 收到 WebSocket 推送消息, userId: {}, msgId: {}", wsMessage.getUserId(), msgId);

        if (wsMessage.getUserId() != null) {
            pushToSingleUser(wsMessage);
        } else if (wsMessage.getUserIds() != null && !wsMessage.getUserIds().isEmpty()) {
            pushToMultipleUsers(wsMessage);
        } else {
            log.warn("[WebSocketPushHandler] 消息中没有指定用户ID，忽略推送, msgId: {}", msgId);
        }
    }

    @Override
    public Class<WebSocketMessage> getDataType() {
        return WebSocketMessage.class;
    }

    private void pushToSingleUser(WebSocketMessage wsMessage) {
        Long userId = wsMessage.getUserId();
        String userIdStr = String.valueOf(userId);

        if (!channelManager.isOnline(userIdStr)) {
            log.debug("[WebSocketPushHandler] 用户 {} 不在本服务器实例, 忽略推送", userId);
            return;
        }

        io.netty.channel.Channel channel = channelManager.getChannel(userIdStr);
        if (channel == null || !channel.isActive()) {
            log.warn("[WebSocketPushHandler] 用户 {} 的 WebSocket 连接不可用", userId);
            return;
        }

        channel.writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(wsMessage)));
        log.info("[WebSocketPushHandler] 成功向用户 {} 推送 WebSocket 消息", userId);
    }

    private void pushToMultipleUsers(WebSocketMessage wsMessage) {
        List<Long> userIds = wsMessage.getUserIds();
        String messageJson = JSONUtil.toJsonStr(wsMessage);
        int successCount = 0;

        for (Long userId : userIds) {
            String userIdStr = String.valueOf(userId);
            if (!channelManager.isOnline(userIdStr)) {
                continue;
            }
            io.netty.channel.Channel channel = channelManager.getChannel(userIdStr);
            if (channel != null && channel.isActive()) {
                channel.writeAndFlush(new TextWebSocketFrame(messageJson));
                successCount++;
            }
        }

        log.info("[WebSocketPushHandler] 成功向 {} 个本地在线用户推送消息 (目标用户总数: {})",
                successCount, userIds.size());
    }
}
