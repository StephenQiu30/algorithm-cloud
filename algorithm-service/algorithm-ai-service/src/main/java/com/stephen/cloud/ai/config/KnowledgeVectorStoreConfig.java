package com.stephen.cloud.ai.config;

import org.elasticsearch.client.RestClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KnowledgeProperties.class)
public class KnowledgeVectorStoreConfig {

    @Bean
    public VectorStore knowledgeVectorStore(RestClient restClient, EmbeddingModel embeddingModel,
            KnowledgeProperties knowledgeProperties) {
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setIndexName(knowledgeProperties.getVectorIndex());
        options.setDimensions(knowledgeProperties.getEmbeddingDimension());
        return ElasticsearchVectorStore.builder(restClient, embeddingModel)
                .options(options)
                .initializeSchema(true)
                .build();
    }
}
