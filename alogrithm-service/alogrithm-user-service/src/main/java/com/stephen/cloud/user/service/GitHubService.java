package com.stephen.cloud.user.service;

import com.stephen.cloud.api.user.model.vo.GitHubUserVO;

/**
 * GitHub 服务
 *
 * @author StephenQiu30
 */
public interface GitHubService {

    /**
     * 根据授权码向 GitHub 换取访问令牌 (access_token)
     *
     * @param code GitHub 授权回调提供的授权码
     * @return 成功返回 access_token，失败返回 null 或抛出异常
     */
    String getAccessToken(String code);

    /**
     * 使用访问令牌获取 GitHub 用户的详细公开信息
     *
     * @param accessToken 有效的 GitHub 访问令牌
     * @return {@link GitHubUserVO} 用户信息视图对象
     */
    GitHubUserVO getUserInfo(String accessToken);
}
