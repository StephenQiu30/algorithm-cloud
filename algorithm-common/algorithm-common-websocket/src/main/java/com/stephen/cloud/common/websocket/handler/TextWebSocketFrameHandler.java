package com.stephen.cloud.common.websocket.handler;

import cn.hutool.json.JSONUtil;
import com.stephen.cloud.common.rabbitmq.enums.WebSocketMessageTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.WebSocketMessage;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;


/**
 * WebSocket处理器，用于处理WebSocket协议中的文本消息
 * 通过继承SimpleChannelInboundHandler，专门处理TextWebSocketFrame类型的数据交换
 * <p>
 * 主要功能：
 * - 处理客户端连接/断开事件
 * - 接收并处理文本消息
 * - 心跳检测
 * - 异常处理
 * <p>
 * 注意：此类不应该使用@Component注解，每个连接应该有独立的handler实例
 *
 * @author StephenQiu30
 */
@Slf4j
public class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final AttributeKey<String> ATTR_USER_ID = AttributeKey.valueOf("ws_user_id");

    private final ChannelManager channelManager;

    /**
     * 构造函数注入 ChannelManager
     */
    public TextWebSocketFrameHandler(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    /**
     * 当有新的客户端连接时被调用
     * 暂不处理，等待客户端发送认证消息
     *
     * @param ctx ChannelHandlerContext
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        log.info("新的 WebSocket 连接建立：{}", ctx.channel().id().asLongText());
    }

    /**
     * 当接收到客户端发送的文本消息时被调用
     * 解析消息类型并进行相应处理
     *
     * @param ctx ChannelHandlerContext
     * @param msg 接收到的 WebSocket 文本消息
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
        try {
            String text = msg.text();
            log.info("收到 WebSocket 消息：{}", text);

            // 解析消息
            WebSocketMessage message = JSONUtil.toBean(text, WebSocketMessage.class);

            // 根据消息类型处理
            if (message.getType() == null) {
                log.warn("消息类型为空，忽略处理");
                return;
            }

            WebSocketMessageTypeEnum typeEnum = WebSocketMessageTypeEnum.getEnumByCode(message.getType());
            if (typeEnum == null) {
                log.warn("未知的消息类型：{}", message.getType());
                return;
            }

            String authedUserId = ctx.channel().attr(ATTR_USER_ID).get();
            if (typeEnum != WebSocketMessageTypeEnum.AUTH && authedUserId == null) {
                sendErrorMessage(ctx, "未认证连接，禁止发送业务消息");
                ctx.close();
                return;
            }

            switch (typeEnum) {
                case AUTH:
                    // 认证消息
                    handleAuth(ctx, message);
                    break;
                case HEARTBEAT:
                    // 心跳消息
                    handleHeartbeat(ctx, authedUserId);
                    break;
                case MESSAGE:
                    // 普通消息
                    handleMessage(ctx, authedUserId, message);
                    break;
                default:
                    log.warn("不支持的消息类型：{}", typeEnum);
            }
        } catch (Exception e) {
            log.error("处理 WebSocket 消息失败", e);
            sendErrorMessage(ctx, "消息处理失败：" + e.getMessage());
        }
    }

    /**
     * 处理认证消息
     */
    private void handleAuth(ChannelHandlerContext ctx, WebSocketMessage message) {
        try {
            String existingUserId = ctx.channel().attr(ATTR_USER_ID).get();
            if (existingUserId != null) {
                log.warn("连接已认证，忽略重复认证请求, userId: {}", existingUserId);
                return;
            }

            // 从消息中获取用户 I D和 Token
            Long userId = message.getUserId();
            String token = message.getToken();

            if (userId == null || token == null) {
                sendErrorMessage(ctx, "认证失败：用户ID或Token不能为空");
                ctx.close();
                return;
            }

            // 验证 Token（使用 Sa-Token）
            if (!validateToken(token, userId)) {
                sendErrorMessage(ctx, "认证失败：Token 无效或已过期");
                ctx.close();
                return;
            }

            // 将用户与Channel绑定（转换为String）
            channelManager.addChannel(String.valueOf(userId), ctx.channel());
            ctx.channel().attr(ATTR_USER_ID).set(String.valueOf(userId));

            // 发送认证成功消息
            WebSocketMessage response = new WebSocketMessage();
            response.setType(WebSocketMessageTypeEnum.AUTH.getCode());
            response.setData("认证成功");
            ctx.channel().writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(response)));

            log.info("用户 {} 认证成功", userId);
        } catch (Exception e) {
            log.error("处理认证消息失败", e);
            sendErrorMessage(ctx, "认证失败：" + e.getMessage());
            ctx.close();
        }
    }

    /**
     * 验证 Token 的有效性
     *
     * @param token  Token
     * @param userId 用户ID
     * @return 是否有效
     */
    private boolean validateToken(String token, Long userId) {
        try {
            Object loginId = cn.dev33.satoken.stp.StpUtil.getLoginIdByToken(token);

            // 验证 Token 对应的用户ID是否匹配
            if (loginId == null) {
                log.warn("Token 无效：未找到登录信息");
                return false;
            }

            // 比较用户ID
            String loginIdStr = String.valueOf(loginId);
            String userIdStr = String.valueOf(userId);

            if (!loginIdStr.equals(userIdStr)) {
                log.warn("Token 验证失败：用户ID不匹配，Token对应用户：{}，请求用户：{}", loginIdStr, userIdStr);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Token 验证异常", e);
            return false;
        }
    }

    /**
     * 处理心跳消息
     */
    private void handleHeartbeat(ChannelHandlerContext ctx, String authedUserId) {
        // 刷新 Redis 中的连接信息（使用已认证的用户ID）
        if (authedUserId != null) {
            channelManager.refreshUserConnection(authedUserId);
        }

        // 回复心跳
        WebSocketMessage response = new WebSocketMessage();
        response.setType(WebSocketMessageTypeEnum.HEARTBEAT.getCode());
        response.setData("pong");
        ctx.channel().writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(response)));
        log.debug("收到心跳消息，已回复");
    }

    /**
     * 处理普通消息
     */
    private void handleMessage(ChannelHandlerContext ctx, String authedUserId, WebSocketMessage message) {
        log.info("收到普通消息, userId: {}, data: {}", authedUserId, message.getData());
        // 这里可以根据业务需求处理消息
        // 例如：转发消息、存储消息等
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(ChannelHandlerContext ctx, String errorMsg) {
        WebSocketMessage response = new WebSocketMessage();
        response.setType(WebSocketMessageTypeEnum.ERROR.getCode());
        response.setData(errorMsg);
        ctx.channel().writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(response)));
    }

    /**
     * 当客户端断开连接时被调用
     * 从ChannelManager中移除该客户端
     *
     * @param ctx ChannelHandlerContext
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        // 从管理器中移除连接
        channelManager.removeChannel(ctx.channel());
        log.info("WebSocket 连接断开：{}", ctx.channel().id().asLongText());
    }

    /**
     * 处理空闲状态事件（心跳检测）
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                // 读空闲，即客户端长时间没有发送数据
                log.warn("连接 {} 读空闲，关闭连接", ctx.channel().id().asLongText());
                ctx.close();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                // 写空闲，服务器主动发送心跳
                WebSocketMessage heartbeat = new WebSocketMessage();
                heartbeat.setType(WebSocketMessageTypeEnum.HEARTBEAT.getCode());
                heartbeat.setData("ping");
                ctx.channel().writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(heartbeat)));
                log.debug("发送服务器心跳到连接 {}", ctx.channel().id().asShortText());
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 当发生异常时被调用
     * 记录异常信息，并向客户端发送错误信息，最后关闭该Channel
     *
     * @param ctx   ChannelHandlerContext
     * @param cause 发生的异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 记录异常信息
        log.error("WebSocket 连接发生异常：{}", cause.getMessage(), cause);
        // 向客户端发送错误信息
        sendErrorMessage(ctx, "服务器错误：" + cause.getMessage());
        // 关闭当前的 Channel 连接
        ctx.close();
    }
}
