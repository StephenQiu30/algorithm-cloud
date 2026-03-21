package com.stephen.cloud.api.mail.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 邮箱验证码请求
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "邮箱验证码请求")
public class EmailCodeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱")
    private String email;

    /**
     * 验证码
     */
    @Schema(description = "验证码")
    private String code;

    /**
     * 客户端IP
     */
    @Schema(description = "客户端IP")
    private String clientIp;
}
