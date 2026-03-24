package com.stephen.cloud.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stephen.cloud.ai.repository.RedisChatMemoryRepository;
import com.stephen.cloud.ai.repository.RedisJdbcChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * AI 模型配置类
 *
 * @author StephenQiu30
 */
@Configuration
public class AiConfig {

    /**
     * 聊天记录内存：
     * 1. 使用 Redis 作为高速缓存 (满足最新的 10 条存储需求)。
     * 2. 使用 JDBC (MySQL) 作为官方持久化存储。
     * 3. 组合为复合仓库，满足 MVP 原则和高性能要求。
     */
    @Bean
    @Primary
    public ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository,
                                 StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        RedisChatMemoryRepository redisRepo = new RedisChatMemoryRepository(redisTemplate, objectMapper);
        RedisJdbcChatMemoryRepository compositeRepo =
                new RedisJdbcChatMemoryRepository(redisRepo, jdbcChatMemoryRepository);
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(compositeRepo)
                .maxMessages(10)
                .build();
    }

    /**
     * AI 对话客户端
     *
     * @param builder 客户端构建器
     * @return {@link ChatClient}
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultAdvisors(advisors -> advisors.advisors(new SimpleLoggerAdvisor()))
                .build();
    }
}
