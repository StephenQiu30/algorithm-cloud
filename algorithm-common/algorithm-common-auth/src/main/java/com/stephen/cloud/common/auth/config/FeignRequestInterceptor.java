package com.stephen.cloud.common.auth.config;

import com.stephen.cloud.common.constants.SecurityConstant;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 请求拦截器
 * 用于在 Feign 调用时添加内部调用标识，并透传认证信息
 *
 * @author StephenQiu30
 */
@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    /**
     * Sa-Token 令牌请求头名称（与 Nacos 配置 token-name 保持一致）
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public void apply(RequestTemplate template) {
        // 1. 添加内部调用标识
        template.header(SecurityConstant.FROM_SOURCE, SecurityConstant.INNER);

        // 2. 透传当前请求的 Authorization token 和 userId，用于服务间认证和身份识别
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            // 透传 Token
            String token = request.getHeader(AUTHORIZATION_HEADER);
            if (token != null && !token.isEmpty()) {
                template.header(AUTHORIZATION_HEADER, token);
            }
            // 透传 UserID
            String userId = request.getHeader(SecurityConstant.USER_ID_HEADER);
            if (userId != null && !userId.isEmpty()) {
                template.header(SecurityConstant.USER_ID_HEADER, userId);
            }

            // 透传 UserName
            String userName = request.getHeader(SecurityConstant.USER_NAME_HEADER);
            if (userName != null && !userName.isEmpty()) {
                template.header(SecurityConstant.USER_NAME_HEADER, userName);
            }
        }
    }
}
