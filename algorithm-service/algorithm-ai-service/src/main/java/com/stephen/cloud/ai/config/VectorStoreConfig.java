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
        return TokenTextSplitter.builder()
                .withChunkSize(documentProcessingProperties.getChunkSize())
                .withMinChunkSizeChars(documentProcessingProperties.getOverlapSize())
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();
    }
}
