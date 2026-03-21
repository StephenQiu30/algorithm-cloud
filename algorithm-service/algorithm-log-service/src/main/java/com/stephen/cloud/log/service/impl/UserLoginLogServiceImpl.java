package com.stephen.cloud.log.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.log.model.dto.login.UserLoginLogAddRequest;
import com.stephen.cloud.api.log.model.dto.login.UserLoginLogQueryRequest;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import com.stephen.cloud.log.mapper.UserLoginLogMapper;
import com.stephen.cloud.log.model.entity.UserLoginLog;
import com.stephen.cloud.log.service.UserLoginLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 用户登录日志服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class UserLoginLogServiceImpl extends ServiceImpl<UserLoginLogMapper, UserLoginLog>
        implements UserLoginLogService {

    @Override
    public boolean addLog(UserLoginLogAddRequest request) {
        if (request == null) {
            log.warn("用户登录日志创建请求为空");
            return false;
        }
        UserLoginLog userLoginLog = new UserLoginLog();
        BeanUtils.copyProperties(request, userLoginLog);
        return this.save(userLoginLog);
    }

    @Override
    public LambdaQueryWrapper<UserLoginLog> getQueryWrapper(UserLoginLogQueryRequest queryRequest) {
        LambdaQueryWrapper<UserLoginLog> queryWrapper = new LambdaQueryWrapper<>();
        if (queryRequest == null) {
            return queryWrapper;
        }
        Long id = queryRequest.getId();
        Long userId = queryRequest.getUserId();
        String account = queryRequest.getAccount();
        String loginType = queryRequest.getLoginType();
        String status = queryRequest.getStatus();
        String clientIp = queryRequest.getClientIp();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();

        queryWrapper.eq(ObjectUtil.isNotNull(id), UserLoginLog::getId, id);
        queryWrapper.eq(ObjectUtil.isNotNull(userId), UserLoginLog::getUserId, userId);
        queryWrapper.eq(StringUtils.isNotBlank(account), UserLoginLog::getAccount, account);
        queryWrapper.eq(StringUtils.isNotBlank(loginType), UserLoginLog::getLoginType, loginType);
        queryWrapper.eq(ObjectUtil.isNotNull(status), UserLoginLog::getStatus, status);
        queryWrapper.eq(StringUtils.isNotBlank(clientIp), UserLoginLog::getClientIp, clientIp);

        if (SqlUtils.validSortField(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> queryWrapper.orderBy(true, isAsc, UserLoginLog::getCreateTime);
                case "updateTime" -> queryWrapper.orderBy(true, isAsc, UserLoginLog::getUpdateTime);
                default -> {
                }
            }
        }
        return queryWrapper;
    }
}
