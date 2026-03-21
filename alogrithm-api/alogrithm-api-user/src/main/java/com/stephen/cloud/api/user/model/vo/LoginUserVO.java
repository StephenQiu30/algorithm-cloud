package com.stephen.cloud.api.user.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 登录用户信息（去除敏感字段，包含token）
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "登录用户信息")
public class LoginUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long id;

    /**
     * 用户昵称
     */
    @Schema(description = "用户昵称")
    private String userName;

    /**
     * 用户头像
     */
    @Schema(description = "用户头像")
    private String userAvatar;

    /**
     * 用户角色：user/admin/ban
     */
    @Schema(description = "用户角色")
    private String userRole;

    /**
     * 用户简介
     */
    @Schema(description = "用户简介")
    private String userProfile;

    /**
     * 用户邮箱
     */
    @Schema(description = "用户邮箱")
    private String userEmail;

    /**
     * 邮箱是否验证：0-未验证，1-已验证
     */
    @Schema(description = "邮箱是否验证")
    private Integer emailVerified;

    /**
     * GitHub 用户名
     */
    @Schema(description = "GitHub用户名")
    private String githubLogin;

    /**
     * GitHub 主页
     */
    @Schema(description = "GitHub主页")
    private String githubUrl;

    /**
     * 用户电话
     */
    @Schema(description = "用户电话")
    private String userPhone;

    /**
     * 最后登录时间
     */
    @Schema(description = "最后登录时间")
    private Date lastLoginTime;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Date updateTime;

    /**
     * 登录token
     */
    @Schema(description = "登录token")
    private String token;
}
