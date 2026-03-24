package com.stephen.cloud.log.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.log.model.dto.access.ApiAccessLogAddRequest;
import com.stephen.cloud.api.log.model.dto.access.ApiAccessLogQueryRequest;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import com.stephen.cloud.log.mapper.ApiAccessLogMapper;
import com.stephen.cloud.log.model.entity.ApiAccessLog;
import com.stephen.cloud.log.service.ApiAccessLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * API访问日志服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class ApiAccessLogServiceImpl extends ServiceImpl<ApiAccessLogMapper, ApiAccessLog>
        implements ApiAccessLogService {

    @Override
    public boolean addLog(ApiAccessLogAddRequest request) {
        if (request == null) {
            log.warn("API访问日志创建请求为空");
            return false;
        }
        ApiAccessLog apiAccessLog = new ApiAccessLog();
        BeanUtils.copyProperties(request, apiAccessLog);
        return this.save(apiAccessLog);
    }

    @Override
    public LambdaQueryWrapper<ApiAccessLog> getQueryWrapper(ApiAccessLogQueryRequest queryRequest) {
        LambdaQueryWrapper<ApiAccessLog> queryWrapper = new LambdaQueryWrapper<>();
        if (queryRequest == null) {
            return queryWrapper;
        }
        Long id = queryRequest.getId();
        String traceId = queryRequest.getTraceId();
        Long userId = queryRequest.getUserId();
        String method = queryRequest.getMethod();
        String path = queryRequest.getPath();
        Integer status = queryRequest.getStatus();
        String clientIp = queryRequest.getClientIp();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();

        queryWrapper.eq(ObjectUtil.isNotNull(id), ApiAccessLog::getId, id);
        queryWrapper.eq(StringUtils.isNotBlank(traceId), ApiAccessLog::getTraceId, traceId);
        queryWrapper.eq(ObjectUtil.isNotNull(userId), ApiAccessLog::getUserId, userId);
        queryWrapper.eq(StringUtils.isNotBlank(method), ApiAccessLog::getMethod, method);
        queryWrapper.like(StringUtils.isNotBlank(path), ApiAccessLog::getPath, path);
        queryWrapper.eq(ObjectUtil.isNotNull(status), ApiAccessLog::getStatus, status);
        queryWrapper.eq(StringUtils.isNotBlank(clientIp), ApiAccessLog::getClientIp, clientIp);

        if (SqlUtils.validSortField(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> queryWrapper.orderBy(true, isAsc, ApiAccessLog::getCreateTime);
                case "updateTime" -> queryWrapper.orderBy(true, isAsc, ApiAccessLog::getUpdateTime);
                default -> {
                }
            }
        }
        return queryWrapper;
    }
}
