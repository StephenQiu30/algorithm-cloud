package com.stephen.cloud.user.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.stephen.cloud.api.user.model.vo.GitHubUserVO;
import com.stephen.cloud.user.config.GitHubProperties;
import com.stephen.cloud.user.service.GitHubService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * GitHub 服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class GitHubServiceImpl implements GitHubService {

    @Resource
    private GitHubProperties gitHubProperties;

    @Override
    public String getAccessToken(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        String url = "https://github.com/login/oauth/access_token";
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("client_id", gitHubProperties.getClientId());
        paramMap.put("client_secret", gitHubProperties.getClientSecret());
        paramMap.put("code", code);
        if (StringUtils.isNotBlank(gitHubProperties.getRedirectUri())) {
            paramMap.put("redirect_uri", gitHubProperties.getRedirectUri());
        }
        try {
            HttpResponse response = HttpRequest.post(url)
                    .form(paramMap)
                    .header("Accept", "application/json")
                    .header("User-Agent", "stephen-cloud")
                    .timeout(5000)
                    .execute();
            if (response.getStatus() != 200) {
                log.warn("GitHub access_token 请求失败，status={}, body={}", response.getStatus(), response.body());
                return null;
            }
            String body = response.body();
            if (StringUtils.isBlank(body)) {
                return null;
            }
            String accessToken = JSONUtil.parseObj(body).getStr("access_token");
            if (StringUtils.isBlank(accessToken)) {
                log.warn("GitHub access_token 响应缺少 token，body={}", body);
            }
            return accessToken;
        } catch (Exception e) {
            log.error("GitHub access_token 请求异常", e);
            return null;
        }
    }

    @Override
    public GitHubUserVO getUserInfo(String accessToken) {
        if (StringUtils.isBlank(accessToken)) {
            return null;
        }
        String url = "https://api.github.com/user";
        try {
            HttpResponse response = HttpRequest.get(url)
                    .header("Authorization", "token " + accessToken)
                    .header("Accept", "application/json")
                    .header("User-Agent", "stephen-cloud")
                    .timeout(5000)
                    .execute();
            if (response.getStatus() != 200) {
                log.warn("GitHub user 信息请求失败，status={}, body={}", response.getStatus(), response.body());
                return null;
            }
            String body = response.body();
            if (StringUtils.isBlank(body)) {
                return null;
            }
            return JSONUtil.toBean(body, GitHubUserVO.class);
        } catch (Exception e) {
            log.error("GitHub user 信息请求异常", e);
            return null;
        }
    }
}
