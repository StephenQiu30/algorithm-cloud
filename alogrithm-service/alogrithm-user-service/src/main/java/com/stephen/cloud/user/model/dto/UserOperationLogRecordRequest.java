package com.stephen.cloud.user.model.dto;

import com.stephen.cloud.user.model.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户操作日志记录请求
 *
 * @author stephen
 */
@Data
@Schema(description = "用户操作日志记录请求")
public class UserOperationLogRecordRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 操作人
     */
    @Schema(description = "操作人")
    private User operator;

    /**
     * 操作模块
     */
    @Schema(description = "操作模块")
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
     * 是否成功
     */
    @Schema(description = "是否成功")
    private Boolean success;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMessage;

    /**
     * HTTP请求（用于获取IP）
     */
    @Schema(description = "HTTP请求")
    private HttpServletRequest httpRequest;
}
