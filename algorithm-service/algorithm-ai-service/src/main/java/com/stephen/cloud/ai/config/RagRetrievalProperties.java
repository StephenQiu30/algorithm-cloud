package com.stephen.cloud.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag.retrieval")
public class RagRetrievalProperties {

    private int topK = 5;

    private Double similarityThreshold = 0.7D;
}
