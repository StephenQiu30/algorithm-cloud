package com.stephen.cloud.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * GitHub OAuth2 配置
 * <p>
 * 配置 GitHub 第三方登录所需的 Client ID、Secret 和回调地址
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "github.oauth2")
public class GitHubProperties {

    /**
     * Client ID
     */
    private String clientId;

    /**
     * Client Secret
     */
    private String clientSecret;

    /**
     * Redirect URI
     */
    private String redirectUri;
}
