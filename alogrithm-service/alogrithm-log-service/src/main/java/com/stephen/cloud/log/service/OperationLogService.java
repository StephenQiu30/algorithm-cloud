package com.stephen.cloud.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.log.model.dto.operation.OperationLogAddRequest;
import com.stephen.cloud.api.log.model.dto.operation.OperationLogQueryRequest;
import com.stephen.cloud.log.model.entity.OperationLog;

/**
 * 操作日志服务
 *
 * @author StephenQiu30
 */
public interface OperationLogService extends IService<OperationLog> {

    /**
     * 添加操作日志
     *
     * @param request 日志创建请求
     * @return 是否添加成功
     */
    boolean addLog(OperationLogAddRequest request);

    /**
     * 根据查询请求构建 MyBatis Plus 的查询条件封装
     *
     * @param queryRequest 操作日志查询请求对象
     * @return LambdaQueryWrapper 查询条件封装
     */
    LambdaQueryWrapper<OperationLog> getQueryWrapper(OperationLogQueryRequest queryRequest);
}
