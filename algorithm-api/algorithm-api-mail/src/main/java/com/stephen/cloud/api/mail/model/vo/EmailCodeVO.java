package com.stephen.cloud.api.mail.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 邮箱验证码响应视图对象
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "邮箱验证码响应")
public class EmailCodeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 验证码过期时间(秒)
     */
    @Schema(description = "验证码过期时间(秒)")
    private Integer expireTime;
}
