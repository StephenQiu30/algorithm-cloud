package com.stephen.cloud.mail.service;

import com.stephen.cloud.api.log.model.dto.email.EmailRecordAddRequest;
import com.stephen.cloud.api.mail.model.dto.MailSendCodeRequest;
import com.stephen.cloud.api.mail.model.dto.MailSendRequest;
import com.stephen.cloud.api.mail.model.enums.EmailStatusEnum;
import com.stephen.cloud.common.rabbitmq.model.EmailMessage;

/**
 * 邮件服务接口
 *
 * @author StephenQiu30
 */
public interface MailService {

    /**
     * 发送简单文本邮件（同步）
     *
     * @param mailSendRequest 邮件发送请求
     */
    void sendSimpleMailSync(MailSendRequest mailSendRequest);

    /**
     * 发送HTML邮件（同步）
     *
     * @param mailSendRequest 邮件发送请求
     */
    void sendHtmlMailSync(MailSendRequest mailSendRequest);

    /**
     * 发送邮件（同步）
     *
     * @param emailMessage 邮件消息
     */
    void sendMailSync(EmailMessage emailMessage);

    /**
     * 发送邮件（异步，通过MQ）
     *
     * @param emailMessage 邮件消息
     */
    void sendMailAsync(EmailMessage emailMessage);

    /**
     * 发送验证码邮件 (通常包含特定的 HTML 模板)
     *
     * @param mailSendCodeRequest 发送验证码邮件请求 (包含邮箱、业务类型等)
     */
    void sendVerificationCode(MailSendCodeRequest mailSendCodeRequest);

    /**
     * 预创建邮件记录（用于分布式事务补偿）
     * 先创建记录（状态 PENDING），发送成功后更新状态为 SUCCESS
     *
     * @param request 邮件记录创建请求
     * @return 邮件记录 ID
     */
    Long createPendingEmail(EmailRecordAddRequest request);

    /**
     * 更新邮件记录状态为成功
     *
     * @param emailRecordId 邮件记录 ID
     */
    void updateEmailStatusToSuccess(Long emailRecordId);

    /**
     * 更新邮件记录状态为失败
     *
     * @param emailRecordId 邮件记录 ID
     * @param errorMessage  错误信息
     */
    void updateEmailStatusToFailed(Long emailRecordId, String errorMessage);

    /**
     * 记录邮件发送日志
     *
     * @param request 邮件记录创建请求
     */
    void recordEmail(EmailRecordAddRequest request);

    /**
     * 根据邮件消息和发送结果记录日志
     *
     * @param emailMessage 邮件消息
     * @param msgId        消息ID
     * @param status       发送状态
     * @param errorMessage 错误信息（成功时为 null）
     */
    void recordEmail(EmailMessage emailMessage, String msgId, EmailStatusEnum status, String errorMessage);
}
