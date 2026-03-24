package com.stephen.cloud.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "chat.memory")
public class ChatMemoryProperties {

    private long redisTtlMinutes = 30;

    private int maxMessages = 16;

    private boolean keepSystemMessages = true;
}
