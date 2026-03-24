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
        try {
            List<Message> redisMessages = redisRepository.findByConversationId(conversationId);
            if (redisMessages != null && !redisMessages.isEmpty()) {
                return redisMessages;
            }
        } catch (Exception e) {
            log.warn("[RedisJdbcChatMemoryRepository] Redis读取失败, conversationId={}", conversationId, e);
        }

        log.info("[RedisJdbcChatMemoryRepository] Redis未命中, 回退JDBC, conversationId={}", conversationId);
        List<Message> jdbcMessages = jdbcRepository.findByConversationId(conversationId);

        if (jdbcMessages != null && !jdbcMessages.isEmpty()) {
            try {
                redisRepository.saveAll(conversationId, jdbcMessages);
            } catch (Exception e) {
                log.warn("[RedisJdbcChatMemoryRepository] JDBC回填Redis失败, conversationId={}", conversationId, e);
            }
        }

        return jdbcMessages;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        try {
            redisRepository.saveAll(conversationId, messages);
        } catch (Exception e) {
            log.warn("[RedisJdbcChatMemoryRepository] 写入Redis失败, conversationId={}", conversationId, e);
        }

        try {
            jdbcRepository.saveAll(conversationId, messages);
        } catch (Exception e) {
            log.error("[RedisJdbcChatMemoryRepository] 写入JDBC失败, conversationId={}", conversationId, e);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        try {
            redisRepository.deleteByConversationId(conversationId);
        } catch (Exception e) {
            log.warn("[RedisJdbcChatMemoryRepository] 删除Redis会话失败, conversationId={}", conversationId, e);
        }
        try {
            jdbcRepository.deleteByConversationId(conversationId);
        } catch (Exception e) {
            log.error("[RedisJdbcChatMemoryRepository] 删除JDBC会话失败, conversationId={}", conversationId, e);
        }
    }
}

