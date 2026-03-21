package com.stephen.cloud.api.user.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户编辑个人信息请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "用户编辑个人信息请求")
public class UserEditRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户昵称
     * 可选，展示给其他用户的昵称
     */
    @Schema(description = "用户昵称")
    private String userName;

    /**
     * 用户头像
     * 可选，头像URL地址
     */
    @Schema(description = "用户头像")
    private String userAvatar;

    /**
     * 用户简介
     * 可选，用户个人描述信息
     */
    @Schema(description = "用户简介")
    private String userProfile;

    /**
     * 用户邮箱
     * 可选，用于登录和接收通知
     */
    @Schema(description = "用户邮箱")
    private String userEmail;

    /**
     * 用户电话
     * 可选，用于联系方式
     */
    @Schema(description = "用户电话")
    private String userPhone;
}
