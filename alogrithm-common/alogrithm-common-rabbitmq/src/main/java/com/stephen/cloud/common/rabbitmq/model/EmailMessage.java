package com.stephen.cloud.common.rabbitmq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 邮件消息模型
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 收件人邮箱
     */
    private String to;

    /**
     * 邮件主题
     */
    private String subject;

    /**
     * 邮件内容
     */
    private String content;

    /**
     * 是否为 HTML 格式
     */
    private Boolean isHtml;

    /**
     * 业务类型（用于日志和监控）
     */
    private String bizType;

    /**
     * 业务ID（用于幂等性控制）
     */
    private String bizId;
}
