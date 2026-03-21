package com.stephen.cloud.common.log.model;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;

/**
 * 操作日志上下文
 * 封装操作日志记录所需的所有信息
 *
 * @author StephenQiu30
 */
@Data
public class OperationLogContext {

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作类型/动作
     */
    private String action;

    /**
     * 请求方法（GET/POST/PUT/DELETE等）
     */
    private String method;

    /**
     * 请求路径
     */
    private String path;

    /**
     * 请求参数（JSON格式）
     */
    private String requestParams;

    /**
     * 响应结果（JSON格式）
     */
    private String responseResult;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * HttpServletRequest对象
     * 可用于获取IP、UserAgent等信息
     */
    private transient HttpServletRequest httpRequest;

    /**
     * 操作人ID
     * 由各服务根据实际情况填充
     */
    private Long operatorId;

    /**
     * 操作人名称
     * 由各服务根据实际情况填充
     */
    private String operatorName;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 归属地
     */
    private String location;
}
