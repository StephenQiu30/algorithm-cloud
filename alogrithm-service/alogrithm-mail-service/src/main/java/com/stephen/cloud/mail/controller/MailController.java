package com.stephen.cloud.mail.controller;

import com.stephen.cloud.api.mail.model.dto.MailSendCodeRequest;
import com.stephen.cloud.api.mail.model.dto.MailSendRequest;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.log.annotation.OperationLog;
import com.stephen.cloud.common.rabbitmq.model.EmailMessage;
import com.stephen.cloud.mail.service.MailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 邮件管理接口
 * <p>
 * 提供通用邮件发送（同步/异步）、验证码模板发送。验证码生成/校验/存储由调用方（如 user-service）负责。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@RestController
@RequestMapping("/mail")
@Tag(name = "MailController", description = "邮件发送")
public class MailController {

    @Resource
    private MailService mailService;

    /**
     * 同步发送邮件
     * <p>
     * 调用底层的 JavaMailSender 立即执行发送流程，请求将在此过程中阻塞。
     * </p>
     *
     * @param request 邮件发送请求
     * @return 成功标志
     */
    @PostMapping("/send/sync")
    @OperationLog(module = "邮件管理", action = "同步发送邮件")
    @Operation(summary = "同步发送邮件", description = "阻塞式发送简单或 HTML 邮件")
    public BaseResponse<Boolean> doSendMailSync(@RequestBody MailSendRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        EmailMessage emailMessage = EmailMessage.builder()
                .to(request.getTo())
                .subject(request.getSubject())
                .content(request.getContent())
                .isHtml(request.getIsHtml())
                .bizType(request.getBizType())
                .bizId(request.getBizId())
                .build();
        mailService.sendMailSync(emailMessage);
        return ResultUtils.success(true);
    }

    /**
     * 异步发送邮件
     * <p>
     * 将邮件任务投递至 RabbitMQ 队列，由消费者异步执行发送。适用于高吞吐场景。
     * </p>
     *
     * @param request 邮件发送请求
     * @return 投递结果
     */
    @PostMapping("/send/async")
    @OperationLog(module = "邮件管理", action = "异步发送邮件")
    @Operation(summary = "异步发送邮件", description = "基于 MQ 分离发送流程，提升接口吞吐量")
    public BaseResponse<Boolean> doSendMailAsync(@RequestBody MailSendRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        EmailMessage emailMessage = EmailMessage.builder()
                .to(request.getTo())
                .subject(request.getSubject())
                .content(request.getContent())
                .isHtml(request.getIsHtml())
                .bizType(request.getBizType())
                .bizId(request.getBizId())
                .build();
        mailService.sendMailAsync(emailMessage);
        return ResultUtils.success(true);
    }

    /**
     * 发送验证码邮件 (底层快捷入口)
     *
     * @param request 请求包
     * @return 是否成功触发
     */
    @PostMapping("/send/verification-code")
    @OperationLog(module = "邮件管理", action = "发送验证码邮件")
    @Operation(summary = "发送验证码邮件", description = "按模板发送验证码内容，验证码由调用方生成并传入")
    public BaseResponse<Boolean> doSendVerificationCode(@RequestBody MailSendCodeRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        mailService.sendVerificationCode(request);
        return ResultUtils.success(true);
    }

}
