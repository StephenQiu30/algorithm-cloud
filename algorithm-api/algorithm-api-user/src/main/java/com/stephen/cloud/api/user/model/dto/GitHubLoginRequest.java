package com.stephen.cloud.api.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * GitHub登录请求
 *
 * @author stephen
 */
@Data
@Schema(description = "GitHub 登录请求")
public class GitHubLoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 授权码
     * 从GitHub OAuth授权流程中获取的临时授权码
     */
    @Schema(description = "授权码")
    private String code;

    /**
     * 防CSRF攻击的随机字符串
     * 用于防止跨站请求伪造攻击
     */
    @Schema(description = "防CSRF攻击的随机字符串")
    private String state;
}
