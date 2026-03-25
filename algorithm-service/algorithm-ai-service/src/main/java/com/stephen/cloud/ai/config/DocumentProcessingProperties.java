package com.stephen.cloud.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "document.processing")
public class DocumentProcessingProperties {

    private int chunkSize = 400;

    private int overlapSize = 80;

    private long maxFileSize = 10485760L;

    private String uploadPath = "uploads/knowledge";

    /**
     * 分段策略：smart（智能切分）、fixed_length（按长度）、by_title（按标题）
     */
    private String chunkStrategy = "smart";

    /**
     * 最大分段长度
     */
    private int maxChunkSize = 800;
}
