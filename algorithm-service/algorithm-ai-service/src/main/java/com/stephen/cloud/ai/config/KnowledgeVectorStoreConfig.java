package com.stephen.cloud.ai.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.elasticsearch.SimilarityFunction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 知识库向量存储与 ES Java API 客户端配置。
 * <p>
 * {@link VectorStore} 使用 Elasticsearch 作向量库，余弦相似度与 text-embedding 类模型常见用法一致；
 * {@link ElasticsearchClient} 与 {@link RestClient} 共用连接，供
 * {@link com.stephen.cloud.ai.service.impl.VectorStoreServiceImpl} 混合检索等执行 DSL。
 * </p>
 *
 * @author StephenQiu30
 */
@Configuration
@EnableConfigurationProperties(KnowledgeProperties.class)
public class KnowledgeVectorStoreConfig {

    /**
     * 与 {@link ElasticsearchVectorStore} 共用同一 {@link RestClient} 的 Java API 客户端，供混合检索 DSL、聚合等扩展使用
     */
    @Bean
    @ConditionalOnMissingBean(ElasticsearchClient.class)
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        return new ElasticsearchClient(new RestClientTransport(restClient,
                new JacksonJsonpMapper(new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false))));
    }

    /**
     * 注册面向知识库的 {@link VectorStore}，索引名与维度来自 {@link KnowledgeProperties}
     *
     * @param restClient          ES 低阶客户端
     * @param embeddingModel      嵌入模型（与 DashScope 等配置联动）
     * @param knowledgeProperties 知识库参数
     * @return 可用于 add / similaritySearch / delete 的向量存储
     */
    @Bean
    public VectorStore knowledgeVectorStore(RestClient restClient, EmbeddingModel embeddingModel,
            KnowledgeProperties knowledgeProperties) {
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setIndexName(knowledgeProperties.getVectorIndex());
        options.setDimensions(knowledgeProperties.getEmbeddingDimension());
        options.setSimilarity(SimilarityFunction.cosine);
        return ElasticsearchVectorStore.builder(restClient, embeddingModel)
                .options(options)
                .initializeSchema(true)
                .build();
    }
}
