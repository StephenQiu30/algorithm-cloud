package com.stephen.cloud.log.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户登录日志实体
 * <p>
 * 记录用户的登录行为，包括成功和失败的登录
 * 用于安全审计和异常登录检测
 * </p>
 *
 * @author StephenQiu30
 */
@TableName(value = "user_login_log")
@Data
@Schema(description = "用户登录日志表")
public class UserLoginLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 登录日志ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "登录日志ID")
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

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    @Schema(description = "是否删除")
    private Integer isDelete;
}
