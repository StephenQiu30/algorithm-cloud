package com.stephen.cloud.mail.mq.handler;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.stephen.cloud.api.mail.model.enums.EmailStatusEnum;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqDedupeLock;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.EmailMessage;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.mail.service.MailService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 邮件发送处理器
 * <p>
 * 遵循策略模式与 MVP 原则，负责 {@link MqBizTypeEnum#EMAIL_SEND} 类型的相关业务执行。
 * 融合 {@link RabbitMqDedupeLock} 声明式解决重复发送问题。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
@RabbitMqDedupeLock(prefix = "mq:email:send", expire = 86400)
public class EmailSendHandler implements RabbitMqHandler<EmailMessage> {

    @Resource
    private MailService mailService;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.EMAIL_SEND.getValue();
    }

    @Override
    public void onMessage(EmailMessage emailMessage, RabbitMessage rabbitMessage) throws Exception {
        String msgId = rabbitMessage.getMsgId();

        if (emailMessage.getTo() == null) {
            log.error("[EmailSendHandler] 邮件内容缺少收件人, msgId: {}", msgId);
            throw new IllegalArgumentException("邮件缺少收件人");
        }

        log.info("[EmailSendHandler] 准备发送邮件, to: {}, subject: {}, msgId: {}",
                emailMessage.getTo(), emailMessage.getSubject(), msgId);

        try {
            mailService.sendMailSync(emailMessage);
            mailService.recordEmail(emailMessage, msgId, EmailStatusEnum.SUCCESS, null);
        } catch (Exception e) {
            log.error("[EmailSendHandler] 邮件发送处理异常, msgId: {}", msgId, e);
            mailService.recordEmail(emailMessage, msgId, EmailStatusEnum.FAILED, ExceptionUtil.getRootCauseMessage(e));
            throw e;
        }
    }

    @Override
    public Class<EmailMessage> getDataType() {
        return EmailMessage.class;
    }
}
