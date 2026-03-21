package com.stephen.cloud.user.service;

/**
 * GitHub OAuth 服务
 *
 * @author StephenQiu30
 */
public interface GitHubOAuthService {

    /**
     * 构建 GitHub OAuth 授权地址 (包含防伪造攻击的 state 参数)
     *
     * @return 完整的 GitHub 授权跳转 URL
     */
    String buildAuthorizeUrl();

    /**
     * 校验并消费 OAuth2 回调中的 state 参数，防止 CSRF 攻击
     *
     * @param state 回调返回的 state 值
     * @throws com.stephen.cloud.common.exception.BusinessException 如果 state
     *                                                              不合法或已过期，则抛出异常
     */
    void validateAndConsumeState(String state);
}
