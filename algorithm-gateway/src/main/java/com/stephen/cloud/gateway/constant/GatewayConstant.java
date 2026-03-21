package com.stephen.cloud.gateway.constant;

/**
 * 网关服务常量
 * <p>
 * 收敛网关层面使用的公共常量，避免魔法值分散在各过滤器中。
 * </p>
 *
 * @author StephenQiu30
 */
public interface GatewayConstant {

    // ==================== Exchange 属性 Key ====================

    /**
     * Exchange 属性：当前登录用户ID（由 GlobalAuthFilter 写入，供 GlobalLogFilter 等消费）
     */
    String ATTR_LOGIN_USER_ID = "loginUserId";

    // ==================== 请求头 ====================

    /**
     * 链路追踪 ID 请求头
     */
    String HEADER_TRACE_ID = "X-Trace-Id";

    // ==================== 日志服务 ====================

    /**
     * 日志服务基础 URL（LoadBalancer 服务名）
     */
    String LOG_SERVICE_BASE_URL = "http://stephen-log-service";

    /**
     * API 访问日志上报接口路径
     */
    String LOG_SERVICE_ACCESS_URI = "/api/log/access/add";
}
