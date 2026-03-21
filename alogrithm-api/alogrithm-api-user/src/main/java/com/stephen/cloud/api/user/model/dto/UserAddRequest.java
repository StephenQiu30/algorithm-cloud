package com.stephen.cloud.api.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户创建请求（管理员）
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "用户创建请求 (管理员)")
public class UserAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * 用户角色
     */
    @Schema(description = "用户角色")
    private String userRole;

    /**
     * 用户邮箱
     */
    @Schema(description = "用户邮箱")
    private String userEmail;
}
