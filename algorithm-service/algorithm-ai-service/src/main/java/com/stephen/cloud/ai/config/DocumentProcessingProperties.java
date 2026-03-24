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
}
