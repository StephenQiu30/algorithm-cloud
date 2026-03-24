package com.stephen.cloud.mail.service.impl;

import com.stephen.cloud.api.log.client.LogFeignClient;
import com.stephen.cloud.api.log.model.dto.email.EmailRecordAddRequest;
import com.stephen.cloud.api.mail.model.dto.EmailAttachment;
import com.stephen.cloud.api.mail.model.dto.MailSendCodeRequest;
import com.stephen.cloud.api.mail.model.dto.MailSendRequest;
import com.stephen.cloud.api.mail.model.enums.EmailStatusEnum;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.EmailMessage;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import com.stephen.cloud.mail.properties.MailProperties;
import com.stephen.cloud.mail.service.EmailTemplateService;
import com.stephen.cloud.mail.service.MailService;
import com.stephen.cloud.mail.validator.AttachmentValidator;
import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 邮件服务实现
 * <p>
 * 提供基础邮件（简单文本、HTML、附件）的发送功能。
 * 支持同步与异步发送，自动集成邮件记录日志。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Service
public class MailServiceImpl implements MailService {

    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private MailProperties mailProperties;

    @Resource
    private RabbitMqSender mqSender;

    @Resource
    @Lazy
    private LogFeignClient logFeignClient;

    @Resource
    private EmailTemplateService emailTemplateService;

    @Resource
    private AttachmentValidator attachmentValidator;

    /**
     * 同步发送简单邮件
     * <p>
     * 适用于内容较为简单的通知性邮件。
     * </p>
     *
     * @param mailSendRequest 邮件发送请求
     */
    @Override
    public void sendSimpleMailSync(MailSendRequest mailSendRequest) {
        String to = mailSendRequest.getTo();
        String subject = mailSendRequest.getSubject();
        String content = mailSendRequest.getContent();
        String bizType = StringUtils.defaultIfBlank(mailSendRequest.getBizType(), "SIMPLE");
        String bizId = mailSendRequest.getBizId();
        validateParams(to, subject, content);

        long startTime = System.currentTimeMillis();
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            String fromAddress = mailProperties.getFromName() + " <" + mailProperties.getFrom() + ">";
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            javaMailSender.send(message);
            long duration = System.currentTimeMillis() - startTime;

            log.info("简单邮件发送成功，收件人：{}，主题：{}，耗时：{}ms", to, subject, duration);

            // 记录邮件发送成功
            recordEmail(EmailRecordAddRequest.builder()
                    .toEmail(to)
                    .subject(subject)
                    .content(content)
                    .isHtml(0)
                    .status(EmailStatusEnum.SUCCESS.getValue())
                    .bizType(bizType)
                    .bizId(bizId)
                    .provider("SYSTEM")
                    .sendTime(new Date())
                    .build());
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String causeMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();

            log.error("简单邮件发送失败，收件人：{}，原因：{}，耗时：{}ms", to, causeMsg, duration, e);

            // 记录邮件发送失败，捕获内部异常避免影响原始异常
            try {
                recordEmail(EmailRecordAddRequest.builder()
                        .toEmail(to)
                        .subject(subject)
                        .content(content)
                        .isHtml(0)
                        .status(EmailStatusEnum.FAILED.getValue())
                        .errorMessage(causeMsg)
                        .bizType(bizType)
                        .bizId(bizId)
                        .provider("SYSTEM")
                        .sendTime(new Date())
                        .build());
            } catch (Exception recordEx) {
                log.error("记录邮件发送失败日志异常", recordEx);
            }

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮件发送失败：" + causeMsg);
        }
    }

    /**
     * 同步发送 HTML 邮件
     * <p>
     * 允许高度定制的内容排版，支持加载 HTML 模板并填充动态变量。
     * 也支持添加附件。
     * </p>
     *
     * @param mailSendRequest 邮件发送请求
     */
    @Override
    public void sendHtmlMailSync(MailSendRequest mailSendRequest) {
        String to = mailSendRequest.getTo();
        String subject = mailSendRequest.getSubject();
        String content = mailSendRequest.getContent();
        String bizType = StringUtils.defaultIfBlank(mailSendRequest.getBizType(), "HTML");
        String bizId = mailSendRequest.getBizId();
        List<EmailAttachment> attachments = mailSendRequest.getAttachments();

        // 如果提供了模板名，则优先进行渲染
        if (StringUtils.isNotBlank(mailSendRequest.getTemplateName())) {
            content = emailTemplateService.renderTemplate(
                    mailSendRequest.getTemplateName(),
                    mailSendRequest.getTemplateVariables());
        }

        validateParams(to, subject, content);

        if (attachments != null && !attachments.isEmpty()) {
            attachmentValidator.validateAll(attachments);
        }

        long startTime = System.currentTimeMillis();
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            if (attachments != null && !attachments.isEmpty()) {
                for (EmailAttachment attachment : attachments) {
                    byte[] decodedBytes = Base64.getDecoder().decode(attachment.getContent());
                    helper.addAttachment(attachment.getFilename(),
                            new ByteArrayResource(decodedBytes));
                }
            }

            javaMailSender.send(mimeMessage);
            long duration = System.currentTimeMillis() - startTime;

            log.info("HTML 邮件发送成功，收件人：{}，主题：{}，附件数：{}，耗时：{}ms",
                    to, subject, attachments != null ? attachments.size() : 0, duration);

            // 记录邮件发送成功
            recordEmail(EmailRecordAddRequest.builder()
                    .toEmail(to)
                    .subject(subject)
                    .content(content)
                    .isHtml(1)
                    .status(EmailStatusEnum.SUCCESS.getValue())
                    .bizType(bizType)
                    .bizId(bizId)
                    .provider("SYSTEM")
                    .sendTime(new Date())
                    .build());
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String causeMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();

            log.error("HTML 邮件发送失败，收件人：{}，原因：{}，耗时：{}ms", to, causeMsg, duration, e);

            // 记录邮件发送失败，捕获内部异常避免影响原始异常
            try {
                recordEmail(EmailRecordAddRequest.builder()
                        .toEmail(to)
                        .subject(subject)
                        .content(content)
                        .isHtml(1)
                        .status(EmailStatusEnum.FAILED.getValue())
                        .errorMessage(causeMsg)
                        .bizType(bizType)
                        .bizId(bizId)
                        .provider("SYSTEM")
                        .sendTime(new Date())
                        .build());
            } catch (Exception recordEx) {
                log.error("记录邮件发送失败日志异常", recordEx);
            }

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮件发送失败：" + causeMsg);
        }
    }

    /**
     * 同步发送邮件（带通用包装）
     * <p>
     * 根据 {@code isHtml} 字段自动路由至不同的发送逻辑。
     * </p>
     *
     * @param emailMessage 邮件消息实体
     */
    @Override
    public void sendMailSync(EmailMessage emailMessage) {
        ThrowUtils.throwIf(emailMessage == null, ErrorCode.PARAMS_ERROR, "邮件消息不能为空");

        MailSendRequest request = MailSendRequest.builder()
                .to(emailMessage.getTo())
                .subject(emailMessage.getSubject())
                .content(emailMessage.getContent())
                .isHtml(emailMessage.getIsHtml())
                .bizType(emailMessage.getBizType())
                .bizId(emailMessage.getBizId())
                .build();

        if (Boolean.TRUE.equals(emailMessage.getIsHtml())) {
            sendHtmlMailSync(request);
        } else {
            sendSimpleMailSync(request);
        }
    }

    /**
     * 异步发送邮件（通过消息队列）
     * <p>
     * 推荐在高并发场景下使用，能有效降低对业务线程的阻塞。
     * </p>
     *
     * @param emailMessage 邮件消息实体
     */
    @Override
    public void sendMailAsync(EmailMessage emailMessage) {
        ThrowUtils.throwIf(emailMessage == null, ErrorCode.PARAMS_ERROR, "邮件消息不能为空");

        try {
            mqSender.send(MqBizTypeEnum.EMAIL_SEND, emailMessage.getBizId(), emailMessage);
            log.info("邮件消息已发送到队列，收件人：{}，主题：{}",
                    emailMessage.getTo(), emailMessage.getSubject());
        } catch (Exception e) {
            log.error("发送邮件到队列失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮件异步投递失败");
        }
    }

    /**
     * 发送验证码邮件
     * <p>
     * 快捷调用，内部加载固定的验证码模板。
     * </p>
     *
     * @param mailSendCodeRequest 发送验证码邮件请求
     */
    @Override
    public void sendVerificationCode(MailSendCodeRequest mailSendCodeRequest) {
        ThrowUtils.throwIf(mailSendCodeRequest == null, ErrorCode.PARAMS_ERROR);
        String to = mailSendCodeRequest.getTo();
        String code = mailSendCodeRequest.getCode();
        Integer minutes = mailSendCodeRequest.getMinutes();
        Boolean async = mailSendCodeRequest.getAsync();

        Map<String, Object> variables = new HashMap<>();
        variables.put("code", code);
        variables.put("minutes", minutes);
        variables.put("action", "登录");

        String content = emailTemplateService.renderTemplate("email/verification-code", variables);

        EmailMessage emailMessage = EmailMessage.builder()
                .to(to)
                .subject("【Stephen Cloud】登录验证码")
                .content(content)
                .isHtml(true)
                .bizType("VERIFICATION_CODE")
                .bizId(to + ":" + System.currentTimeMillis() + ":" + UUID.randomUUID())
                .build();

        if (Boolean.TRUE.equals(async)) {
            sendMailAsync(emailMessage);
        } else {
            sendMailSync(emailMessage);
        }
    }

    /**
     * 参数基础校验
     *
     * @param to      收件人
     * @param subject 主题
     * @param content 正文
     */
    private void validateParams(String to, String subject, String content) {
        ThrowUtils.throwIf(StringUtils.isAnyBlank(to, subject, content), ErrorCode.PARAMS_ERROR, "邮件核心参数不能为空");
    }

    /**
     * 记录邮件发送日志
     * <p>
     * 通过 Feign 调用日志服务进行数据落地。
     * </p>
     *
     * @param request 邮件记录创建请求
     */
    @Override
    public void recordEmail(EmailRecordAddRequest request) {
        if (request == null) {
            return;
        }
        try {
            if (StringUtils.isBlank(request.getMsgId())) {
                request.setMsgId(UUID.randomUUID().toString());
            }
            if (request.getSendTime() == null) {
                request.setSendTime(new Date());
            }
            logFeignClient.addEmailRecord(request);
        } catch (Exception e) {
            log.error("[MailServiceImpl] 记录邮件异步日志失败", e);
        }
    }

    @Override
    public void recordEmail(EmailMessage emailMessage, String msgId, EmailStatusEnum status, String errorMessage) {
        recordEmail(EmailRecordAddRequest.builder()
                .msgId(msgId)
                .toEmail(emailMessage.getTo())
                .subject(emailMessage.getSubject())
                .content(emailMessage.getContent())
                .isHtml(Boolean.TRUE.equals(emailMessage.getIsHtml()) ? 1 : 0)
                .status(status.getValue())
                .errorMessage(errorMessage)
                .bizType(emailMessage.getBizType())
                .bizId(emailMessage.getBizId())
                .provider("MQ")
                .sendTime(new Date())
                .build());
    }


}
