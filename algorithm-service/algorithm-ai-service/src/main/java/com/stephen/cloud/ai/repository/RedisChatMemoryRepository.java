package com.stephen.cloud.ai.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 自定义 Redis 对话记忆仓库
 * <p>
 * 遵循 MVP 原则，利用 Redis 的高性能和 TTL 特性存储对话上下文。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final String KEY_PREFIX = "chat:memory:";
    private static final long DEFAULT_TTL_MINUTES = 30;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatMemoryRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<String> findConversationIds() {
        // 由于通常不需要遍历所有会话，此方法简单实现或返回空
        return new ArrayList<>();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = getKeys(conversationId);
        List<String> jsonMessages = redisTemplate.opsForList().range(key, 0, -1);
        if (jsonMessages == null || jsonMessages.isEmpty()) {
            return new ArrayList<>();
        }

        return jsonMessages.stream()
                .map(this::toMessage)
                .collect(Collectors.toList());
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        String key = getKeys(conversationId);
        
        // 清除旧数据，重新写入（维护最新的快照）
        redisTemplate.delete(key);
        
        List<String> jsonMessages = messages.stream()
                .map(this::toJson)
                .collect(Collectors.toList());
        
        redisTemplate.opsForList().rightPushAll(key, jsonMessages);
        redisTemplate.expire(key, DEFAULT_TTL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redisTemplate.delete(getKeys(conversationId));
    }

    private String getKeys(String conversationId) {
        return KEY_PREFIX + conversationId;
    }

    private String toJson(Message message) {
        try {
            MessageDto dto = new MessageDto();
            dto.setContent(message.getText());
            dto.setType(message.getMessageType().name());
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("Serialize message to json failed", e);
            return null;
        }
    }

    private Message toMessage(String json) {
        try {
            MessageDto dto = objectMapper.readValue(json, MessageDto.class);
            return switch (MessageType.valueOf(dto.getType())) {
                case USER -> new UserMessage(dto.getContent());
                case ASSISTANT -> new AssistantMessage(dto.getContent());
                case SYSTEM -> new SystemMessage(dto.getContent());
                default -> new UserMessage(dto.getContent()); // 降级处理
            };
        } catch (JsonProcessingException e) {
            log.error("Deserialize json to message failed", e);
            return null;
        }
    }

    /**
     * 简单的消息传输对象，避免直接序列化复杂的 Message 接口
     */
    public static class MessageDto {
        private String content;
        private String type;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
