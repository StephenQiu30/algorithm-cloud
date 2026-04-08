package com.stephen.cloud.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 聊天记忆配置属性
 * <p>
 * 控制会话历史的保留时间和最大消息数量
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Component
@ConfigurationProperties(prefix = "chat.memory")
public class ChatMemoryProperties {

    private long redisTtlMinutes = 30;

    private int maxMessages = 16;

    private boolean keepSystemMessages = true;
}
