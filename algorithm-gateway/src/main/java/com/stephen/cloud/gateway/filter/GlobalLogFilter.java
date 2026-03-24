package com.stephen.cloud.gateway.filter;

import com.stephen.cloud.api.log.model.dto.access.ApiAccessLogAddRequest;
import com.stephen.cloud.common.constants.SecurityConstant;
import com.stephen.cloud.gateway.constant.GatewayConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * 日志全局过滤器
 * <p>
 * 记录每个请求的起止时间、状态码、客户端IP 等信息，
 * 并在响应完成后异步上报到日志服务（algorithm-log-service）。
 * </p>
 *
 * <p>
 * 执行顺序: 最先执行（order = -200），早于认证过滤器
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class GlobalLogFilter implements GlobalFilter, Ordered {

    private final WebClient logWebClient;

    public GlobalLogFilter(WebClient.Builder webClientBuilder) {
        this.logWebClient = webClientBuilder
                .baseUrl(GatewayConstant.LOG_SERVICE_BASE_URL)
                .build();
    }

    /**
     * 过滤器核心逻辑
     * 1. 记录请求进入的时间
     * 2. 生成并注入 TraceId
     * 3. 提取请求元数据（IP, Path, Method, User-Agent 等）
     * 4. 在响应链路完成后，计算耗时并异步提交访问日志
     *
     * @param exchange ServerWebExchange
     * @param chain    GatewayFilterChain
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 记录请求开始时间
        long startTime = System.currentTimeMillis();

        // 生成链路追踪ID
        String traceId = UUID.randomUUID().toString();

        // 提取请求基本信息
        final String path = request.getPath().value();
        final String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        final String query = request.getURI().getQuery();
        final String clientIp = resolveClientIp(request);
        final String userAgent = request.getHeaders().getFirst("User-Agent");
        final String referer = request.getHeaders().getFirst("Referer");

        // 将 traceId 注入下游请求头，实现链路追踪
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(GatewayConstant.HEADER_TRACE_ID, traceId)
                .build();

        log.info("[Log] ==> {} {}?{} from {} [TraceID: {}]", method, path, query, clientIp, traceId);

        // 执行后续过滤器链，并在响应完成后记录日志
        return chain.filter(exchange.mutate().request(mutatedRequest).build()).then(
                Mono.fromRunnable(() -> {
                    long latencyMs = System.currentTimeMillis() - startTime;

                    int statusCode = 0;
                    if (exchange.getResponse().getStatusCode() != null) {
                        statusCode = exchange.getResponse().getStatusCode().value();
                    }

                    // 从 Exchange 属性中获取登录用户ID（由 GlobalAuthFilter 写入）
                    Long loginUserId = resolveLoginUserId(exchange);

                    log.info("[Log] <== {} {} [{}] {}ms [TraceID: {}]",
                            method, path, statusCode, latencyMs, traceId);

                    // 异步上报到日志服务
                    submitAccessLog(traceId, path, method, query,
                            statusCode, (int) latencyMs, clientIp, userAgent, referer, loginUserId);
                }));
    }

    /**
     * 解析客户端真实IP
     * <p>
     * 优先从 X-Forwarded-For / X-Real-IP 请求头获取（经过反向代理时），
     * 否则取 remoteAddress
     * </p>
     */
    private String resolveClientIp(ServerHttpRequest request) {
        // 尝试从代理头获取真实IP
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For 可能包含多个IP，取第一个
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        // 从 remoteAddress 获取
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "unknown";
    }

    /**
     * 从 Exchange 属性中解析登录用户ID
     */
    private Long resolveLoginUserId(ServerWebExchange exchange) {
        Object attr = exchange.getAttribute(GatewayConstant.ATTR_LOGIN_USER_ID);
        if (attr == null) {
            return null;
        }
        try {
            return Long.parseLong(attr.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 异步上报 API 访问日志到日志服务（fire-and-forget，不阻塞主链路）
     */
    private void submitAccessLog(String traceId, String path, String method,
                                 String query, int status, int latencyMs,
                                 String clientIp, String userAgent, String referer,
                                 Long loginUserId) {
        try {
            ApiAccessLogAddRequest request = new ApiAccessLogAddRequest();
            request.setTraceId(traceId);
            if (loginUserId != null) {
                request.setUserId(loginUserId);
            }
            request.setMethod(method);
            request.setPath(path);
            request.setQuery(query);
            request.setStatus(status);
            request.setLatencyMs(latencyMs);
            request.setClientIp(clientIp);
            request.setUserAgent(userAgent);
            request.setReferer(referer);

            logWebClient.post()
                    .uri(GatewayConstant.LOG_SERVICE_ACCESS_URI)
                    .header(SecurityConstant.FROM_SOURCE, SecurityConstant.INNER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .onErrorResume(e -> {
                        log.error("[Log] 上报访问日志失败: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("[Log] 构建访问日志请求失败", e);
        }
    }

    @Override
    public int getOrder() {
        // 最先执行的过滤器之一，在认证之前记录请求开始
        return -200;
    }
}
