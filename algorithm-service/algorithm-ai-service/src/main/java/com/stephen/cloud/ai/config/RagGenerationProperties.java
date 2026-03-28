package com.stephen.cloud.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag.generation")
public class RagGenerationProperties {

    /**
     * 注入到模型中的最大上下文长度（字符预算）
     */
    private int maxContextLength = 4000;

    /**
     * 回答生成温度
     */
    private Double temperature = 0.7D;
}
