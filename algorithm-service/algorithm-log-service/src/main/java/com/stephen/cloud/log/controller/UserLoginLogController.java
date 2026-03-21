package com.stephen.cloud.log.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.stephen.cloud.common.auth.annotation.InternalAuth;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.log.model.dto.login.UserLoginLogAddRequest;
import com.stephen.cloud.api.log.model.dto.login.UserLoginLogQueryRequest;
import com.stephen.cloud.api.log.model.vo.UserLoginLogVO;
import com.stephen.cloud.common.common.*;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.log.convert.LogConvert;
import com.stephen.cloud.log.model.entity.UserLoginLog;
import com.stephen.cloud.log.service.UserLoginLogService;
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
 * 用户登录日志接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/log/login")
@Slf4j
@Tag(name = "UserLoginLogController", description = "用户登录日志接口")
public class UserLoginLogController {

    @Resource
    private UserLoginLogService userLoginLogService;

    /**
     * 创建用户登录日志
     *
     * @param request 日志创建请求
     * @return 是否创建成功
     */
    @PostMapping("/add")
    @InternalAuth
    @Operation(summary = "创建用户登录日志", description = "记录用户登录日志")
    public BaseResponse<Boolean> addUserLoginLog(@RequestBody UserLoginLogAddRequest request) {
        boolean result = userLoginLogService.addLog(request);
        return ResultUtils.success(result);
    }

    /**
     * 删除用户登录日志
     *
     * @param deleteRequest 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "删除用户登录日志", description = "删除指定用户登录日志（仅管理员）")
    public BaseResponse<Boolean> deleteUserLoginLog(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        UserLoginLog oldLog = userLoginLogService.getById(id);
        ThrowUtils.throwIf(oldLog == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = userLoginLogService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 分页获取用户登录日志列表（仅管理员）
     *
     * @param queryRequest 查询请求
     * @return 分页日志列表
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "分页获取用户登录日志列表", description = "分页查询用户登录日志（仅管理员）")
    public BaseResponse<Page<UserLoginLogVO>> listLogByPage(@RequestBody UserLoginLogQueryRequest queryRequest) {
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<UserLoginLog> logPage = userLoginLogService.page(new Page<>(current, size),
                userLoginLogService.getQueryWrapper(queryRequest));
        Page<UserLoginLogVO> voPage = new Page<>(current, size, logPage.getTotal());
        List<UserLoginLogVO> voList = logPage.getRecords().stream()
                .map(LogConvert::userLoginLogToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }
}
