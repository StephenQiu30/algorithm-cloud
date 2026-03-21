package com.stephen.cloud.search.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Elasticsearch 配置属性
 *
 * @author StephenQiu30
 */
@Data
@ConfigurationProperties(prefix = "spring.elasticsearch")
public class ElasticsearchProperties {

    /**
     * 是否启用索引管理功能
     */
    private boolean enable = true;

    /**
     * 冲突时是否强制重建索引（生产环境慎用）
     */
    private boolean recreateOnConflict = false;

    /**
     * ES 地址列表
     */
    private List<String> uris;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

}
