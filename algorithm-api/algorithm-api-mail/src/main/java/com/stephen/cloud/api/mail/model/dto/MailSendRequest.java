package com.stephen.cloud.api.mail.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 邮件发送请求
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "邮件发送请求")
public class MailSendRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 收件人
     */
    @Schema(description = "收件人")
    private String to;

    /**
     * 主题
     */
    @Schema(description = "主题")
    private String subject;

    /**
     * 内容
     */
    @Schema(description = "内容")
    private String content;

    /**
     * 是否为HTML格式
     */
    @Schema(description = "是否为HTML格式")
    private Boolean isHtml;

    /**
     * 业务类型
     */
    @Schema(description = "业务类型")
    private String bizType;

    /**
     * 业务ID
     */
    @Schema(description = "业务ID")
    private String bizId;

    /**
     * 模板名称
     */
    @Schema(description = "模板名称")
    private String templateName;

    /**
     * 模板变量
     */
    @Schema(description = "模板变量")
    private Map<String, Object> templateVariables;

    /**
     * 附件列表
     */
    @Schema(description = "附件列表")
    private List<EmailAttachment> attachments;
}
