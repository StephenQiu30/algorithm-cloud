package com.stephen.cloud.log.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.stephen.cloud.common.auth.annotation.InternalAuth;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.log.model.dto.email.EmailRecordAddRequest;
import com.stephen.cloud.api.log.model.dto.email.EmailRecordQueryRequest;

import com.stephen.cloud.api.log.model.vo.EmailRecordVO;
import com.stephen.cloud.common.common.*;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.log.convert.LogConvert;
import com.stephen.cloud.log.model.entity.EmailRecord;
import com.stephen.cloud.log.service.EmailRecordService;
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
 * 邮件记录接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/log/email")
@Slf4j
@Tag(name = "EmailRecordController", description = "邮件记录接口")
public class EmailRecordController {

    @Resource
    private EmailRecordService emailRecordService;

    /**
     * 创建邮件记录
     *
     * @param request 创建请求
     * @return 是否创建成功
     */
    @PostMapping("/add")
    @InternalAuth
    @Operation(summary = "创建邮件记录", description = "记录邮件发送信息")
    public BaseResponse<Boolean> addEmailRecord(@RequestBody EmailRecordAddRequest request) {
        boolean result = emailRecordService.addRecord(request);
        return ResultUtils.success(result);
    }

    /**
     * 删除邮件记录
     *
     * @param deleteRequest 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "删除邮件记录", description = "删除指定邮件记录（仅管理员）")
    public BaseResponse<Boolean> deleteEmailRecord(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        EmailRecord oldRecord = emailRecordService.getById(id);
        ThrowUtils.throwIf(oldRecord == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = emailRecordService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 分页获取邮件记录列表（仅管理员）
     *
     * @param queryRequest 查询请求
     * @return 分页记录列表
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "分页获取邮件记录列表", description = "分页查询邮件记录（仅管理员）")
    public BaseResponse<Page<EmailRecordVO>> listRecordByPage(@RequestBody EmailRecordQueryRequest queryRequest) {
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<EmailRecord> recordPage = emailRecordService.page(new Page<>(current, size),
                emailRecordService.getQueryWrapper(queryRequest));
        Page<EmailRecordVO> voPage = new Page<>(current, size, recordPage.getTotal());
        List<EmailRecordVO> voList = recordPage.getRecords().stream()
                .map(LogConvert::emailRecordToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }
}
