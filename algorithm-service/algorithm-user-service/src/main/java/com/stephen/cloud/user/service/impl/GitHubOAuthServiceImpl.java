package com.stephen.cloud.user.service.impl;

import com.stephen.cloud.common.cache.constants.KeyPrefixConstants;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.user.config.GitHubProperties;
import com.stephen.cloud.user.service.GitHubOAuthService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * GitHub OAuth 服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class GitHubOAuthServiceImpl implements GitHubOAuthService {

    /**
     * GitHub OAuth state 过期时间（秒）
     */
    private static final long STATE_EXPIRE_SECONDS = 10 * 60L;

    @Resource
    private CacheUtils cacheUtils;

    @Resource
    private GitHubProperties gitHubProperties;

    @Override
    public String buildAuthorizeUrl() {
        String clientId = gitHubProperties.getClientId();
        ThrowUtils.throwIf(StringUtils.isBlank(clientId), ErrorCode.OPERATION_ERROR, "GitHub ClientId 未配置");
        String redirectUri = gitHubProperties.getRedirectUri();

        String state = UUID.randomUUID().toString().replace("-", "");
        String stateKey = KeyPrefixConstants.GITHUB_OAUTH_STATE + state;
        cacheUtils.putString(stateKey, "1", STATE_EXPIRE_SECONDS);

        StringBuilder urlBuilder = new StringBuilder("https://github.com/login/oauth/authorize");
        urlBuilder.append("?client_id=").append(clientId);
        if (StringUtils.isNotBlank(redirectUri)) {
            urlBuilder.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
        }
        urlBuilder.append("&state=").append(state);
        return urlBuilder.toString();
    }

    @Override
    public void validateAndConsumeState(String state) {
        ThrowUtils.throwIf(StringUtils.isBlank(state), ErrorCode.PARAMS_ERROR, "state 不能为空");
        String stateKey = KeyPrefixConstants.GITHUB_OAUTH_STATE + state;
        boolean exists = cacheUtils.exists(stateKey);
        ThrowUtils.throwIf(!exists, ErrorCode.PARAMS_ERROR, "state 已失效");
        cacheUtils.remove(stateKey);
    }
}
