package com.stephen.cloud.search.manager;

import cn.hutool.core.io.resource.ResourceUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.stephen.cloud.api.search.constant.EsIndexConstant;
import com.stephen.cloud.common.elasticsearch.properties.ElasticsearchProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * ES 索引管理器
 * 负责自动创建和同步 ES 索引结构
 *
 * @author stephen
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "spring.elasticsearch", name = "enable", havingValue = "true", matchIfMissing = true)
public class EsIndexManager {

    @Resource
    private ElasticsearchClient elasticsearchClient;

    @Resource
    private ElasticsearchProperties elasticsearchProperties;

    /**
     * 索引名称与资源文件的映射
     */
    private static final Map<String, String> INDEX_RESOURCE_MAP = new HashMap<>();

    static {
        INDEX_RESOURCE_MAP.put(EsIndexConstant.POST_INDEX, "es/indices/post.json");
        INDEX_RESOURCE_MAP.put(EsIndexConstant.USER_INDEX, "es/indices/user.json");
        INDEX_RESOURCE_MAP.put(EsIndexConstant.CHUNK_INDEX, "es/indices/chunk.json");
    }

    /**
     * 项目启动时自动执行同步
     */
    @PostConstruct
    public void init() {
        log.info("[EsIndexManager] 开始初始化 ES 索引...");
        INDEX_RESOURCE_MAP.forEach(this::syncIndex);
        log.info("[EsIndexManager] ES 索引初始化完成");
    }

    /**
     * 同步单个索引 (简化版：如果存在则跳过)
     *
     * @param indexName    索引名称
     * @param resourcePath 资源路径
     */
    public void syncIndex(String indexName, String resourcePath) {
        try {
            // 1. 检查索引是否存在
            boolean exists = elasticsearchClient.indices().exists(e -> e.index(indexName)).value();

            // 2. 如果已存在，直接跳过 (满足用户：如果存在的话跳过即可)
            if (exists) {
                log.info("[EsIndexManager] 索引 [{}] 已存在，跳过初始化。", indexName);
                return;
            }

            // 3. 加载配置文件内容
            String jsonContent = ResourceUtil.readUtf8Str(resourcePath);
            if (jsonContent == null) {
                log.warn("[EsIndexManager] 配置文件不存在: {}", resourcePath);
                return;
            }

            // 4. 创建索引
            log.info("[EsIndexManager] 索引 [{}] 不存在，正在创建...", indexName);
            elasticsearchClient.indices().create(c -> c
                    .index(indexName)
                    .withJson(new StringReader(jsonContent)));
            log.info("[EsIndexManager] 索引 [{}] 创建成功", indexName);

        } catch (Exception e) {
            log.error("[EsIndexManager] 同步索引 [{}] 失败", indexName, e);
        }
    }
}
