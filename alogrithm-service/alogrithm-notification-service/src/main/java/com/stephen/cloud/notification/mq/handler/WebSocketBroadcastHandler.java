package com.stephen.cloud.notification.mq.handler;

import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * WebSocket 广播处理器 (全服广播)
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class WebSocketBroadcastHandler implements RabbitMqHandler<Object> {

    @Resource
    private ChannelManager channelManager;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.WEBSOCKET_BROADCAST.getValue();
    }

    @Override
    public void onMessage(Object data, RabbitMessage rabbitMessage) throws Exception {
        String msgId = rabbitMessage.getMsgId();

        log.info("[WebSocketBroadcastHandler] 收到 WebSocket 广播消息, msgId: {}", msgId);

        // 广播的内容仍然在 msgText 中
        channelManager.getAllChannels().writeAndFlush(new TextWebSocketFrame(rabbitMessage.getMsgText()));

        log.info("[WebSocketBroadcastHandler] 成功广播消息给所有在线用户, 在线人数: {}, msgId: {}",
                channelManager.getOnlineCount(), msgId);
    }

    @Override
    public Class<Object> getDataType() {
        return Object.class;
    }
}
