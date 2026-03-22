package com.stephen.cloud.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.knowledge")
public class KnowledgeProperties {

    private String vectorIndex = "algorithm-knowledge-vectors";

    private int embeddingDimension = 1536;

    private int chunkSize = 800;

    private int chunkOverlap = 100;

    private int defaultTopK = 5;

    private String storageDir = System.getProperty("java.io.tmpdir") + "/algorithm-kb-uploads";

    private String embeddingModelName = "text-embedding-v2";
}
