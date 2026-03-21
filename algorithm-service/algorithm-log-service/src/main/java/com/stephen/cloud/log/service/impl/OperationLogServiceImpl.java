package com.stephen.cloud.log.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.log.model.dto.operation.OperationLogAddRequest;
import com.stephen.cloud.api.log.model.dto.operation.OperationLogQueryRequest;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import com.stephen.cloud.log.mapper.OperationLogMapper;
import com.stephen.cloud.log.model.entity.OperationLog;
import com.stephen.cloud.log.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog>
        implements OperationLogService {

    @Override
    public boolean addLog(OperationLogAddRequest request) {
        if (request == null) {
            log.warn("操作日志创建请求为空");
            return false;
        }
        OperationLog operationLog = new OperationLog();
        BeanUtils.copyProperties(request, operationLog);
        return this.save(operationLog);
    }

    @Override
    public LambdaQueryWrapper<OperationLog> getQueryWrapper(OperationLogQueryRequest queryRequest) {
        LambdaQueryWrapper<OperationLog> queryWrapper = new LambdaQueryWrapper<>();
        if (queryRequest == null) {
            return queryWrapper;
        }
        Long id = queryRequest.getId();
        Long operatorId = queryRequest.getOperatorId();
        String module = queryRequest.getModule();
        String action = queryRequest.getAction();
        Integer success = queryRequest.getSuccess();
        String clientIp = queryRequest.getClientIp();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();

        queryWrapper.eq(ObjectUtil.isNotNull(id), OperationLog::getId, id);
        queryWrapper.eq(ObjectUtil.isNotNull(operatorId), OperationLog::getOperatorId, operatorId);
        queryWrapper.eq(StringUtils.isNotBlank(module), OperationLog::getModule, module);
        queryWrapper.like(StringUtils.isNotBlank(action), OperationLog::getAction, action);
        queryWrapper.eq(ObjectUtil.isNotNull(success), OperationLog::getSuccess, success);
        queryWrapper.eq(StringUtils.isNotBlank(clientIp), OperationLog::getClientIp, clientIp);

        if (SqlUtils.validSortField(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> queryWrapper.orderBy(true, isAsc, OperationLog::getCreateTime);
                case "updateTime" -> queryWrapper.orderBy(true, isAsc, OperationLog::getUpdateTime);
                default -> {
                }
            }
        }
        return queryWrapper;
    }
}
