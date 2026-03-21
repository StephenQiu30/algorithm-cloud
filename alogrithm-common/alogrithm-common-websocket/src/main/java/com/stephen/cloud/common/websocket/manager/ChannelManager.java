package com.stephen.cloud.common.websocket.manager;

import com.stephen.cloud.common.cache.utils.CacheUtils;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 连接管理器
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class ChannelManager {

    @Resource
    private CacheUtils cacheUtils;

    /**
     * 本地连接映射：userId -> Channel
     */
    private final Map<String, Channel> userChannelMap = new ConcurrentHashMap<>();

    /**
     * 本地连接映射：channelId -> userId
     */
    private final Map<String, String> channelUserMap = new ConcurrentHashMap<>();

    /**
     * 所有连接的 Channel 组
     */
    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * Redis Key 前缀
     */
    private static final String REDIS_KEY_PREFIX = "ws:user:";

    /**
     * 连接信息过期时间（秒）
     */
    private static final int EXPIRE_TIME = 86400; // 24小时

    /**
     * 添加连接
     *
     * @param userId  用户 ID
     * @param channel Channel
     */
    public void addChannel(String userId, Channel channel) {
        // 先检查是否存在旧连接，如果存在则关闭
        Channel oldChannel = userChannelMap.get(userId);
        if (oldChannel != null && oldChannel.isActive()) {
            log.warn("用户 {} 存在旧连接，关闭旧连接 {}", userId, oldChannel.id().asShortText());
            // 从映射中移除旧连接
            channelUserMap.remove(oldChannel.id().asLongText());
            channels.remove(oldChannel);
            // 关闭旧连接
            oldChannel.close();
        }

        // 添加到本地映射
        userChannelMap.put(userId, channel);
        channelUserMap.put(channel.id().asLongText(), userId);
        channels.add(channel);

        // 存储到 Redis
        String key = REDIS_KEY_PREFIX + userId;
        Map<String, String> connectionInfo = new HashMap<>();
        connectionInfo.put("channelId", channel.id().asLongText());
        connectionInfo.put("connectTime", String.valueOf(System.currentTimeMillis()));

        // 使用 CacheUtils 存储（设置过期时间，由心跳机制续期）
        cacheUtils.setHash(key, connectionInfo, EXPIRE_TIME);

        log.info("用户连接成功, userId: {}, channelId: {}", userId, channel.id().asShortText());
    }

    /**
     * 移除连接
     *
     * @param channel Channel
     */
    public void removeChannel(Channel channel) {
        String channelId = channel.id().asLongText();
        String userId = channelUserMap.get(channelId);

        if (userId != null) {
            // 从本地映射中移除
            userChannelMap.remove(userId);
            channelUserMap.remove(channelId);
            channels.remove(channel);

            // 从 Redis 中移除
            String key = REDIS_KEY_PREFIX + userId;
            cacheUtils.delete(key);

            log.info("用户断开连接, userId: {}, channelId: {}", userId, channel.id().asShortText());
        }
    }

    /**
     * 获取用户的 Channel
     *
     * @param userId 用户 ID
     * @return Channel
     */
    public Channel getChannel(String userId) {
        return userChannelMap.get(userId);
    }

    /**
     * 检查用户是否在线
     *
     * @param userId 用户 ID
     * @return 是否在线
     */
    public boolean isOnline(String userId) {
        Channel channel = userChannelMap.get(userId);
        return channel != null && channel.isActive();
    }

    /**
     * 获取所有连接的 Channel 组
     *
     * @return ChannelGroup
     */
    public ChannelGroup getAllChannels() {
        return channels;
    }

    /**
     * 获取在线用户数
     *
     * @return 在线用户数
     */
    public int getOnlineCount() {
        return userChannelMap.size();
    }

    /**
     * 获取所有在线用户 ID
     *
     * @return 在线用户 ID 列表
     */
    public java.util.List<String> getOnlineUserIds() {
        return new java.util.ArrayList<>(userChannelMap.keySet());
    }

    /**
     * 刷新用户连接的 Redis 过期时间
     *
     * @param userId 用户 ID
     */
    public void refreshUserConnection(String userId) {
        String key = REDIS_KEY_PREFIX + userId;
        // 刷新 Redis 中的连接信息，延长过期时间
        if (cacheUtils.exists(key)) {
            // 重新获取连接信息并设置过期时间
            Map<String, String> connectionInfo = cacheUtils.getHash(key);
            if (connectionInfo != null && !connectionInfo.isEmpty()) {
                cacheUtils.setHash(key, connectionInfo, EXPIRE_TIME);
            }
        }
    }
}
