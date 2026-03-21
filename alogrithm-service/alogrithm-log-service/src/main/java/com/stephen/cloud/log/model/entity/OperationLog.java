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
 * 操作日志实体
 * <p>
 * 记录用户的操作行为，用于审计和追溯
 * 包含操作人、操作类型、请求参数、响应结果等信息
 * </p>
 *
 * @author StephenQiu30
 */
@TableName(value = "operation_log")
@Data
@Schema(description = "操作日志表")
public class OperationLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "日志ID")
    private Long id;

    /**
     * 操作人ID
     */
    @Schema(description = "操作人ID")
    private Long operatorId;

    /**
     * 操作人名称
     */
    @Schema(description = "操作人名称")
    private String operatorName;

    /**
     * 模块
     */
    @Schema(description = "模块")
    private String module;

    /**
     * 操作类型
     */
    @Schema(description = "操作类型")
    private String action;

    /**
     * HTTP方法
     */
    @Schema(description = "HTTP方法")
    private String method;

    /**
     * 请求路径
     */
    @Schema(description = "请求路径")
    private String path;

    /**
     * 请求参数
     */
    @Schema(description = "请求参数")
    private String requestParams;

    /**
     * 响应状态码
     */
    @Schema(description = "响应状态码")
    private Integer responseStatus;

    /**
     * 是否成功
     */
    @Schema(description = "是否成功")
    private Integer success;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMessage;

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
