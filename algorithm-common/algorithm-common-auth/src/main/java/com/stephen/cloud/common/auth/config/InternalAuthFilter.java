package com.stephen.cloud.common.auth.config;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.stephen.cloud.common.constants.SecurityConstant;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 内部调用鉴权过滤器
 * 如果是内部调用，则直接赋予系统管理员身份，绕过 Sa-Token 的所有校验（包含 AOP 注解校验）
 *
 * @author stephen
 */
@Component
@Order(-90)
public class InternalAuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String fromSource = httpRequest.getHeader(SecurityConstant.FROM_SOURCE);

        if (StrUtil.equals(fromSource, SecurityConstant.INNER)) {
            // 优先从请求头获取原始用户 ID，实现身份透传
            String userIdStr = httpRequest.getHeader(SecurityConstant.USER_ID_HEADER);
            long loginId = StrUtil.isNotBlank(userIdStr) ? Long.parseLong(userIdStr) : 0L;

            // 使用对应身份切换，仅对当前线程有效
            StpUtil.switchTo(loginId, () -> {
                try {
                    chain.doFilter(request, response);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            chain.doFilter(request, response);
        }
    }
}
