package com.stephen.cloud.common.elasticsearch.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "spring.elasticsearch")
public class ElasticsearchProperties {

    private boolean enable = true;

    private boolean recreateOnConflict = false;

    private List<String> uris;

    private String username;

    private String password;

}
