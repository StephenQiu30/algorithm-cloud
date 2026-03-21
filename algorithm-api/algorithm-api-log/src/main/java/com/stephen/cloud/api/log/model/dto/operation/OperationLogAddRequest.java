package com.stephen.cloud.api.log.model.dto.operation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 操作日志创建请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "操作日志创建请求")
public class OperationLogAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
}
