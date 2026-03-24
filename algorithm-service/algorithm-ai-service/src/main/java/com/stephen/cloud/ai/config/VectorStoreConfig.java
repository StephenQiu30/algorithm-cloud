package com.stephen.cloud.ai.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter(800, 100, 5, 10000, true);
    }
}
