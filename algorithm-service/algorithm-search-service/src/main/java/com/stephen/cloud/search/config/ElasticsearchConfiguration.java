package com.stephen.cloud.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stephen.cloud.search.config.properties.ElasticsearchProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Elasticsearch 配置
 * 手动配置 RestClient 以确保认证信息被正确应用，解决 Actuator 401 问题
 *
 * @author StephenQiu30
 */
@Configuration
@EnableConfigurationProperties(ElasticsearchProperties.class)
@ConditionalOnProperty(prefix = "spring.elasticsearch", name = "enable", havingValue = "true", matchIfMissing = true)
@Slf4j
public class ElasticsearchConfiguration {

    @Resource
    private ElasticsearchProperties elasticsearchProperties;

    @Bean
    @Primary
    public RestClient restClient() {
        List<String> uris = elasticsearchProperties.getUris();
        if (uris == null || uris.isEmpty()) {
            uris = List.of("http://localhost:9200");
        }

        // 调试日志：验证 Nacos 属性是否正确注入
        log.info("[Elasticsearch] 正在初始化 RestClient. Hosts: {}, User: {}",
                uris, elasticsearchProperties.getUsername());

        HttpHost[] hosts = uris.stream()
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);

        RestClientBuilder builder = RestClient.builder(hosts);

        // 配置连接超时
        builder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                .setConnectTimeout(10000)
                .setSocketTimeout(60000));

        // 核心修复：手动配置认证信息
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            if (StringUtils.isNotBlank(elasticsearchProperties.getUsername())) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(
                                elasticsearchProperties.getUsername(),
                                elasticsearchProperties.getPassword()));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                log.info("[Elasticsearch] 已为 RestClient 开启基本认证");
            } else {
                log.warn("[Elasticsearch] 未检测到用户名，RestClient 将以匿名方式运行");
            }
            httpClientBuilder.setMaxConnTotal(100).setMaxConnPerRoute(50);
            return httpClientBuilder;
        });

        return builder.build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JacksonJsonpMapper mapper = new JacksonJsonpMapper(objectMapper);
        RestClientTransport transport = new RestClientTransport(restClient, mapper);
        log.info("[Elasticsearch] ElasticsearchClient 初始化成功");
        return new ElasticsearchClient(transport);
    }

}
