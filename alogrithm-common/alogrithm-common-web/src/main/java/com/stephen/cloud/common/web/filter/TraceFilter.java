package com.stephen.cloud.common.web.filter;

import cn.hutool.core.util.StrUtil;
import com.stephen.cloud.common.core.constant.TraceConstants;
import com.stephen.cloud.common.core.utils.TraceIdUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import java.io.IOException;

/**
 * 链路追踪过滤器
 *
 * @author StephenQiu30
 */
public class TraceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 获取请求头中的 traceId
            String traceId = request.getHeader(TraceConstants.TRACE_ID_HEADER);
            if (StrUtil.isBlank(traceId)) {
                // 如果没有，生成新的
                traceId = TraceIdUtils.setupTraceId();
            } else {
                // 如果有，使用现有的
                TraceIdUtils.setupTraceId(traceId);
            }

            // 将 traceId 写入响应头，方便联调
            response.setHeader(TraceConstants.TRACE_ID_HEADER, traceId);

            filterChain.doFilter(request, response);
        } finally {
            // 清理 MDC，防止内存泄漏和日志串扰
            TraceIdUtils.clearTraceId();
        }
    }
}
