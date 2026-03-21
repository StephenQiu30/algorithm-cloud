package com.stephen.cloud.common.core.constant;

/**
 * 链路追踪常量
 *
 * @author StephenQiu30
 */
public interface TraceConstants {

    /**
     * 链路追踪自增长 ID
     */
    String TRACE_ID = "traceId";

    /**
     * HTTP 响应头中的 TraceId
     */
    String TRACE_ID_HEADER = "X-Trace-Id";

}
