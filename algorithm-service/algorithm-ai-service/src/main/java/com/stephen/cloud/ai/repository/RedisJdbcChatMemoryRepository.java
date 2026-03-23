package com.stephen.cloud.ai.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

@Slf4j
public class RedisJdbcChatMemoryRepository implements ChatMemoryRepository {

    private final ChatMemoryRepository redisRepository;
    private final ChatMemoryRepository jdbcRepository;

    public RedisJdbcChatMemoryRepository(ChatMemoryRepository redisRepository, ChatMemoryRepository jdbcRepository) {
        this.redisRepository = redisRepository;
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public List<String> findConversationIds() {
        return jdbcRepository.findConversationIds();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<Message> messages = redisRepository.findByConversationId(conversationId);
        if (messages != null && !messages.isEmpty()) {
            return messages;
        }

        log.info("Redis cache miss for conversation {}, falling back to MySQL", conversationId);
        messages = jdbcRepository.findByConversationId(conversationId);

        if (messages != null && !messages.isEmpty()) {
            redisRepository.saveAll(conversationId, messages);
        }

        return messages;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        try {
            redisRepository.saveAll(conversationId, messages);
        } catch (Exception e) {
            log.warn("Failed to save conversation {} to Redis", conversationId, e);
        }

        try {
            jdbcRepository.saveAll(conversationId, messages);
        } catch (Exception e) {
            log.error("Failed to save conversation {} to MySQL (Official JDBC)", conversationId, e);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redisRepository.deleteByConversationId(conversationId);
        jdbcRepository.deleteByConversationId(conversationId);
    }
}

