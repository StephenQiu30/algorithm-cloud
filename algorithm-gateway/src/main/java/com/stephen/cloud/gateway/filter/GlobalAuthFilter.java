package com.stephen.cloud.gateway.filter;

import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.constants.SecurityConstant;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.gateway.constant.GatewayConstant;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 认证全局过滤器
 * <p>
 * 对所有非白名单请求进行 Sa-Token 认证检查，验证通过后将用户信息注入到下游请求头中。
 * 白名单通过 {@code spring.cloud.gateway.auth.white-list} 配置。
 * </p>
 *
 * <p>
 * 执行顺序: {@link GlobalHeaderSanitizeFilter} → 本过滤器 → {@link GlobalLogFilter}
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "spring.cloud.gateway.auth")
public class GlobalAuthFilter implements GlobalFilter, Ordered {

    /**
     * 白名单路径（不需要认证），支持 Ant 风格通配符
     */
    @Setter
    private List<String> whiteList = new ArrayList<>();

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    /**
     * 执行过滤逻辑
     * 1. 放行 OPTIONS 预检请求
     * 2. 放行配置的白名单路径
     * 3. 拦截其他所有请求并进行 Sa-Token 认证
     * 4. 认证通过后，将用户信息注入请求头
     *
     * @param exchange ServerWebExchange
     * @param chain GatewayFilterChain
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";

        // 放行 OPTIONS 预检请求（CORS）
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return chain.filter(exchange);
        }

        // 放行白名单路径
        if (isWhiteListed(path)) {
            log.debug("[Auth] 白名单放行: {} {}", method, path);
            return chain.filter(exchange);
        }

        // 使用 SaReactorSyncHolder 包装，确保 SaToken 上下文在 WebFlux 环境中可用
        return SaReactorSyncHolder.setContext(exchange, () -> {
            try {
                // 1. 检查 token 是否存在
                String token = StpUtil.getTokenValue();
                if (StrUtil.isBlank(token)) {
                    log.warn("[Auth] 未携带 token: {} {}", method, path);
                    return writeUnauthorizedResponse(exchange.getResponse(), "未登录");
                }

                // 2. 校验 token 合法性
                StpUtil.checkLogin();

                // 3. 提取用户信息并注入到下游请求头
                String userId = StpUtil.getLoginIdAsString();
                UserVO userVO = (UserVO) StpUtil.getSession().get(UserConstant.USER_LOGIN_STATE);
                String userName = (userVO != null) ? userVO.getUserName() : "";

                // 将用户ID存入 Exchange 属性，供 GlobalLogFilter 等后续过滤器使用
                exchange.getAttributes().put(GatewayConstant.ATTR_LOGIN_USER_ID, userId);

                ServerHttpRequest mutatedRequest = request.mutate()
                        .header(SecurityConstant.USER_ID_HEADER, userId)
                        .header(SecurityConstant.USER_NAME_HEADER, userName)
                        .build();

                log.info("[Auth] 认证通过: userId={}, userName={}, path={}", userId, userName, path);
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            } catch (Exception e) {
                log.warn("[Auth] token 验证失败: {} {}, reason={}", method, path, e.getMessage());
                return writeUnauthorizedResponse(exchange.getResponse(), "登录已过期");
            }
        });
    }

    /**
     * 判断请求路径是否匹配白名单
     */
    private boolean isWhiteListed(String path) {
        if (path == null) {
            return false;
        }
        return whiteList.stream()
                .anyMatch(pattern -> pattern != null && ANT_PATH_MATCHER.match(pattern, path));
    }

    /**
     * 构造 401 未授权 JSON 响应
     */
    private Mono<Void> writeUnauthorizedResponse(@NonNull ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        BaseResponse<Void> baseResponse = ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, message);
        String body = JSONUtil.toJsonStr(baseResponse);
        if (body == null) {
            body = "{\"code\":40100,\"message\":\"" + message + "\"}";
        }

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // SaTokenContextFilterForReactor 的 order 是 -99，必须在其之后执行
        return -98;
    }
}
