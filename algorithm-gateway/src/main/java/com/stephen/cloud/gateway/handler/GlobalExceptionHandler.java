package com.stephen.cloud.gateway.handler;

import cn.hutool.json.JSONUtil;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * 全局异常处理器
 * <p>
 * 统一处理网关层面的异常，将异常信息转换为标准 JSON 响应格式。
 * 覆盖常见场景：HTTP 状态异常、服务不可达、请求超时等。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        // 响应已提交，无法修改
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 根据异常类型确定 HTTP 状态码和错误信息
        int code;
        String message;
        HttpStatus httpStatus;

        if (ex instanceof ResponseStatusException responseStatusException) {
            HttpStatusCode statusCode = responseStatusException.getStatusCode();
            httpStatus = HttpStatus.resolve(statusCode.value());
            if (httpStatus == null) {
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            code = httpStatus.value();
            message = responseStatusException.getReason();
        } else if (ex instanceof ConnectException) {
            // 服务不可达
            httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
            code = ErrorCode.SYSTEM_ERROR.getCode();
            message = "服务不可用，请稍后再试";
            log.error("[Gateway] 服务连接失败: {}", ex.getMessage());
        } else if (ex instanceof TimeoutException) {
            // 请求超时
            httpStatus = HttpStatus.GATEWAY_TIMEOUT;
            code = ErrorCode.SYSTEM_ERROR.getCode();
            message = "请求超时，请稍后再试";
            log.error("[Gateway] 请求超时: {}", ex.getMessage());
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            code = ErrorCode.SYSTEM_ERROR.getCode();
            message = "系统内部错误";
        }

        response.setStatusCode(httpStatus);

        log.error("[Gateway] 异常处理: status={}, message={}", httpStatus.value(), message, ex);

        // 构造标准 JSON 响应体
        BaseResponse<Void> baseResponse = ResultUtils.error(code, message);
        String body = JSONUtil.toJsonStr(baseResponse);
        if (body == null) {
            body = "{\"code\":" + code + ",\"message\":\"" + message + "\"}";
        }

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
