package com.stephen.cloud.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 生成阶段配置属性
 * <p>
 * 控制注入 LLM 的上下文窗口大小和生成温度，通过 Nacos 动态注入。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag.generation")
public class RagGenerationProperties {

    /**
     * 注入到模型中的最大上下文长度（字符预算）
     */
    private int maxContextLength = 8000;

    /**
     * 回答生成温度
     */
    private Double temperature = 0.4D;
}
