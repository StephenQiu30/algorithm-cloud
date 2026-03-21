package com.stephen.cloud.api.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 发送邮箱验证码请求
 *
 * @author stephen
 */
@Data
@Schema(description = "发送邮箱验证码请求")
public class UserEmailCodeSendRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱")
    private String email;
}
