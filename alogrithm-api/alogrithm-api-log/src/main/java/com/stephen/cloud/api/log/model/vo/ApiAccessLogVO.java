package com.stephen.cloud.api.log.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * API访问日志VO
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "API访问日志")
public class ApiAccessLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "主键")
    private Long id;

    /**
     * 链路追踪ID
     */
    @Schema(description = "链路追踪ID")
    private String traceId;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

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
     * 查询参数
     */
    @Schema(description = "查询参数")
    private String query;

    /**
     * 响应状态码
     */
    @Schema(description = "响应状态码")
    private Integer status;

    /**
     * 请求耗时(ms)
     */
    @Schema(description = "请求耗时(ms)")
    private Integer latencyMs;

    /**
     * 客户端IP
     */
    @Schema(description = "客户端IP")
    private String clientIp;

    /**
     * User-Agent
     */
    @Schema(description = "User-Agent")
    private String userAgent;

    /**
     * Referer
     */
    @Schema(description = "Referer")
    private String referer;

    /**
     * 请求大小
     */
    @Schema(description = "请求大小")
    private Long requestSize;

    /**
     * 响应大小
     */
    @Schema(description = "响应大小")
    private Long responseSize;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;
}
