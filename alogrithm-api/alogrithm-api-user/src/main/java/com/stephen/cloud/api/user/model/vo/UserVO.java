package com.stephen.cloud.api.user.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户视图对象
 * 用于API数据传输
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "用户视图对象")
public class UserVO implements Serializable {

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
     * 用户简介
     */
    @Schema(description = "用户简介")
    private String userProfile;

    /**
     * 用户角色：user/admin/ban
     */
    @Schema(description = "用户角色")
    private String userRole;

    /**
     * 用户邮箱
     */
    @Schema(description = "用户邮箱")
    private String userEmail;

    /**
     * 用户电话
     */
    @Schema(description = "用户电话")
    private String userPhone;

    /**
     * GitHub 登录账号
     */
    @Schema(description = "GitHub 登录账号")
    private String githubLogin;

    /**
     * GitHub 主页
     */
    @Schema(description = "GitHub 主页")
    private String githubUrl;

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
}
