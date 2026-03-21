package com.stephen.cloud.mail.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮件配置属性
 *
 * @author StephenQiu30
 */
@Data
@Component
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    /**
     * 发件人邮箱
     */
    private String from = "noreply@stephen-cloud.com";

    /**
     * 发件人名称
     */
    private String fromName = "StephenQiu30";

    /**
     * 是否启用异步发送（通过MQ）
     */
    private Boolean asyncEnabled = true;

    /**
     * 单个附件最大大小（MB）
     */
    private Integer maxAttachmentSize = 50;

    /**
     * 附件总大小限制（MB）
     */
    private Integer maxTotalAttachmentSize = 50;
}
