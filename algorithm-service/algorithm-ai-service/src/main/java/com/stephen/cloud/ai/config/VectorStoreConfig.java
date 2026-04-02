package com.stephen.cloud.ai.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.Resource;

/**
 * 向量存储相关 Bean 配置
 * <p>
 * 注册 TokenTextSplitter（fixed_length 策略使用），参数从 Nacos 动态读取。
 * </p>
 *
 * @author StephenQiu30
 */
@Configuration
public class VectorStoreConfig {

    @Resource
    private DocumentProcessingProperties documentProcessingProperties;

    /**
     * Spring AI TokenTextSplitter Bean（fixed_length 策略使用）
     * <p>
     * chunkSize 从 Nacos 配置动态读取；minChunkSizeChars 固定 100，
     * 丢弃过短的碎片分片，保证每个 chunk 有足够语义密度。
     * </p>
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(documentProcessingProperties.getChunkSize())
                // minChunkSizeChars: 短于此长度的分片将被丢弃，避免无语义碎片
                .withMinChunkSizeChars(100)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();
    }
}
