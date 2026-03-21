package com.stephen.cloud.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.log.model.dto.login.UserLoginLogAddRequest;
import com.stephen.cloud.api.log.model.dto.login.UserLoginLogQueryRequest;
import com.stephen.cloud.log.model.entity.UserLoginLog;

/**
 * 用户登录日志服务
 *
 * @author StephenQiu30
 */
public interface UserLoginLogService extends IService<UserLoginLog> {

    /**
     * 添加用户登录日志
     *
     * @param request 日志创建请求
     * @return 是否添加成功
     */
    boolean addLog(UserLoginLogAddRequest request);

    /**
     * 根据查询请求构建 MyBatis Plus 的查询条件封装
     *
     * @param queryRequest 用户登录日志查询请求对象
     * @return LambdaQueryWrapper 查询条件封装
     */
    LambdaQueryWrapper<UserLoginLog> getQueryWrapper(UserLoginLogQueryRequest queryRequest);
}
