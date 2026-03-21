package com.stephen.cloud.api.log.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户登录日志VO
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "用户登录日志")
public class UserLoginLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "主键")
    private Long id;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 登录账号
     */
    @Schema(description = "登录账号")
    private String account;

    /**
     * 登录类型
     */
    @Schema(description = "登录类型")
    private String loginType;

    /**
     * 登录状态
     */
    @Schema(description = "登录状态")
    private String status;

    /**
     * 失败原因
     */
    @Schema(description = "失败原因")
    private String failReason;

    /**
     * 客户端IP
     */
    @Schema(description = "客户端IP")
    private String clientIp;

    /**
     * 归属地
     */
    @Schema(description = "归属地")
    private String location;

    /**
     * User-Agent
     */
    @Schema(description = "User-Agent")
    private String userAgent;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;
}
