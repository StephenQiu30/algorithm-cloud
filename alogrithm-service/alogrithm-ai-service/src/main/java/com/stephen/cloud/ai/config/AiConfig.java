package com.stephen.cloud.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 模型配置类
 *
 * @author StephenQiu30
 */
@Configuration
public class AiConfig {

    /**
     * AI 对话客户端
     *
     * @param builder 客户端构建器
     * @return {@link ChatClient}
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
