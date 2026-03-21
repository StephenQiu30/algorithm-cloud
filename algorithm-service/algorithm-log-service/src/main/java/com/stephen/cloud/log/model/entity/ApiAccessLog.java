package com.stephen.cloud.log.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * API访问日志实体
 * <p>
 * 记录所有API接口的访问情况
 * 用于性能分析、问题排查和流量统计
 * </p>
 *
 * @author StephenQiu30
 */
@TableName(value = "api_access_log")
@Data
@Schema(description = "API访问日志")
public class ApiAccessLog implements Serializable {

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
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
