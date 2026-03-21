package com.stephen.cloud.common.log.service;

import com.stephen.cloud.common.log.model.OperationLogContext;

/**
 * 操作日志记录器接口
 * <p>
 * 各个微服务需实现此接口以提供具体的日志记录逻辑。
 * </p>
 *
 * @author StephenQiu30
 */
public interface OperationLogRecorder {

    /**
     * 异步记录操作日志
     *
     * @param context 操作日志上下文
     */
    void recordOperationLogAsync(OperationLogContext context);
}
