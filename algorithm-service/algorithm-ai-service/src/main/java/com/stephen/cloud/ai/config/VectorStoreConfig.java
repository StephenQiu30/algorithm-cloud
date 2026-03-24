package com.stephen.cloud.ai.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.Resource;

@Configuration
public class VectorStoreConfig {

    @Resource
    private DocumentProcessingProperties documentProcessingProperties;

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter(
                documentProcessingProperties.getChunkSize(),
                documentProcessingProperties.getOverlapSize(),
                5,
                10000,
                true
        );
    }
}
