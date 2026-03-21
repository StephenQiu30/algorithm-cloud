package com.stephen.cloud.gateway.filter;

import com.stephen.cloud.common.constants.SecurityConstant;
import com.stephen.cloud.gateway.constant.GatewayConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 请求头净化过滤器（最高优先级）
 * <p>
 * 强制剥离外部请求中可能携带的内部调用敏感请求头，防止伪造攻击。
 * 这些请求头应该由网关在认证通过后统一注入，不允许外部直接传入。
 * </p>
 *
 * @author StephenQiu30
 * @see GlobalAuthFilter
 */
@Slf4j
@Component
public class GlobalHeaderSanitizeFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 剥离外部请求中的敏感请求头：内部调用标识、用户ID、用户名
        ServerHttpRequest sanitizedRequest = request.mutate()
                .headers(httpHeaders -> {
                    httpHeaders.remove(SecurityConstant.FROM_SOURCE);
                    httpHeaders.remove(SecurityConstant.USER_ID_HEADER);
                    httpHeaders.remove(SecurityConstant.USER_NAME_HEADER);
                    httpHeaders.remove(GatewayConstant.HEADER_TRACE_ID);
                })
                .build();

        if (log.isDebugEnabled()) {
            log.debug("[HeaderSanitize] 已剥离敏感请求头 [Path: {}]", request.getPath());
        }

        return chain.filter(exchange.mutate().request(sanitizedRequest).build());
    }

    @Override
    public int getOrder() {
        // 最高优先级，确保在所有过滤器之前执行
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
