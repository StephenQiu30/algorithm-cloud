package com.stephen.cloud.log.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.stephen.cloud.common.auth.annotation.InternalAuth;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.log.model.dto.access.ApiAccessLogAddRequest;
import com.stephen.cloud.api.log.model.dto.access.ApiAccessLogQueryRequest;
import com.stephen.cloud.api.log.model.vo.ApiAccessLogVO;
import com.stephen.cloud.common.common.*;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.log.convert.LogConvert;
import com.stephen.cloud.log.model.entity.ApiAccessLog;
import com.stephen.cloud.log.service.ApiAccessLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * API访问日志接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/log/access")
@Slf4j
@Tag(name = "ApiAccessLogController", description = "API 访问日志接口")
public class ApiAccessLogController {

    @Resource
    private ApiAccessLogService apiAccessLogService;

    /**
     * 创建 API 访问日志
     *
     * @param request 日志创建请求
     * @return 是否创建成功
     */
    @PostMapping("/add")
    @InternalAuth
    @Operation(summary = "创建API访问日志", description = "记录API访问日志")
    public BaseResponse<Boolean> addApiAccessLog(@RequestBody ApiAccessLogAddRequest request) {
        boolean result = apiAccessLogService.addLog(request);
        return ResultUtils.success(result);
    }

    /**
     * 删除 API 访问日志
     *
     * @param deleteRequest 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "删除API访问日志", description = "删除指定API访问日志（仅管理员）")
    public BaseResponse<Boolean> deleteApiAccessLog(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        ApiAccessLog oldLog = apiAccessLogService.getById(id);
        ThrowUtils.throwIf(oldLog == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = apiAccessLogService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 分页获取API访问日志列表（仅管理员）
     *
     * @param queryRequest 查询请求
     * @return 分页日志列表
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "分页获取API访问日志列表", description = "分页查询API访问日志（仅管理员）")
    public BaseResponse<Page<ApiAccessLogVO>> listLogByPage(@RequestBody ApiAccessLogQueryRequest queryRequest) {
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<ApiAccessLog> logPage = apiAccessLogService.page(new Page<>(current, size),
                apiAccessLogService.getQueryWrapper(queryRequest));
        Page<ApiAccessLogVO> voPage = new Page<>(current, size, logPage.getTotal());
        List<ApiAccessLogVO> voList = logPage.getRecords().stream()
                .map(LogConvert::apiAccessLogToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }
}
