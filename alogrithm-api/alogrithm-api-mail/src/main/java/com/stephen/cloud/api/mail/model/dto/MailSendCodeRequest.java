package com.stephen.cloud.api.mail.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 发送验证码邮件请求
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "发送验证码邮件请求")
public class MailSendCodeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 收件人
     */
    @Schema(description = "收件人")
    private String to;

    /**
     * 验证码
     */
    @Schema(description = "验证码")
    private String code;

    /**
     * 有效期(分钟)
     */
    @Schema(description = "有效期(分钟)")
    private Integer minutes;

    /**
     * 是否异步发送
     */
    @Schema(description = "是否异步发送")
    private Boolean async;
}
