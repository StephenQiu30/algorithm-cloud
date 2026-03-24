package com.stephen.cloud.common.log.service;

import cn.hutool.core.util.StrUtil;
import com.stephen.cloud.api.log.client.LogFeignClient;
import com.stephen.cloud.api.log.model.dto.operation.OperationLogAddRequest;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.log.model.OperationLogContext;
import com.stephen.cloud.common.utils.IpUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(LogFeignClient.class)
@Slf4j
public class OperationLogRecorderImpl implements OperationLogRecorder {

    @Resource
    private LogFeignClient logFeignClient;

    @Async
    @Override
    public void recordOperationLogAsync(OperationLogContext context) {
        try {
            if (context == null) {
                return;
            }

            OperationLogAddRequest request = new OperationLogAddRequest();
            request.setModule(context.getModule());
            request.setAction(context.getAction());
            request.setMethod(context.getMethod());
            request.setPath(context.getPath());
            request.setRequestParams(context.getRequestParams());
            request.setSuccess(Boolean.TRUE.equals(context.getSuccess()) ? 1 : 0);
            request.setErrorMessage(context.getErrorMessage());

            if (context.getOperatorId() != null) {
                request.setOperatorId(context.getOperatorId());
            }
            if (StrUtil.isNotBlank(context.getOperatorName())) {
                request.setOperatorName(context.getOperatorName());
            }

            String clientIp = context.getClientIp();
            if (StrUtil.isNotBlank(clientIp)) {
                request.setClientIp(clientIp);
            }

            String location = context.getLocation();
            if (StrUtil.isBlank(location) && StrUtil.isNotBlank(clientIp)) {
                location = IpUtils.getRegion(clientIp);
            }
            if (StrUtil.isNotBlank(location)) {
                request.setLocation(location);
            }

            BaseResponse<Boolean> response = logFeignClient.addOperationLog(request);
            if (response == null || response.getCode() != ErrorCode.SUCCESS.getCode()
                    || !Boolean.TRUE.equals(response.getData())) {
                log.error("操作日志落库失败, module={}, action={}, path={}, code={}, message={}",
                        context.getModule(), context.getAction(), context.getPath(),
                        response == null ? "null" : response.getCode(),
                        response == null ? "empty response" : response.getMessage());
            }
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }
}

