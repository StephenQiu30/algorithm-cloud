package com.stephen.cloud.user.model.dto;

import com.stephen.cloud.api.log.model.enums.LoginTypeEnum;
import com.stephen.cloud.user.model.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户登录日志记录请求
 *
 * @author stephen
 */
@Data
@Schema(description = "用户登录日志记录请求")
public class UserLoginLogRecordRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户
     */
    @Schema(description = "用户")
    private User user;

    /**
     * 登录类型
     */
    @Schema(description = "登录类型")
    private LoginTypeEnum loginType;

    /**
     * 账号（GitHub用户名、邮箱、微信OpenID等）
     */
    @Schema(description = "账号")
    private String account;

    /**
     * HTTP请求（用于获取IP和UserAgent）
     */
    @Schema(description = "HTTP请求")
    private HttpServletRequest httpRequest;
}
