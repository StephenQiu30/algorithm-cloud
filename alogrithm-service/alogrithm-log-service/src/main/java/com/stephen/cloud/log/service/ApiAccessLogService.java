package com.stephen.cloud.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.log.model.dto.access.ApiAccessLogAddRequest;
import com.stephen.cloud.api.log.model.dto.access.ApiAccessLogQueryRequest;
import com.stephen.cloud.log.model.entity.ApiAccessLog;

/**
 * API 访问日志服务
 *
 * @author StephenQiu30
 */
public interface ApiAccessLogService extends IService<ApiAccessLog> {

    /**
     * 添加 API 访问日志
     *
     * @param request 日志创建请求
     * @return 是否添加成功
     */
    boolean addLog(ApiAccessLogAddRequest request);

    /**
     * 根据查询请求构建 MyBatis Plus 的查询条件封装
     *
     * @param queryRequest API 访问日志查询请求对象
     * @return LambdaQueryWrapper 查询条件封装
     */
    LambdaQueryWrapper<ApiAccessLog> getQueryWrapper(ApiAccessLogQueryRequest queryRequest);
}
