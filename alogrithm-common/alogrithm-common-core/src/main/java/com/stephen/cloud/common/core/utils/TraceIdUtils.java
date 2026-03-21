package com.stephen.cloud.common.core.utils;

import cn.hutool.core.util.IdUtil;
import com.stephen.cloud.common.core.constant.TraceConstants;
import org.slf4j.MDC;

/**
 * 链路追踪工具类
 *
 * @author StephenQiu30
 */
public class TraceIdUtils {

    /**
     * 生成并设置 traceId
     */
    public static String setupTraceId() {
        String traceId = IdUtil.fastSimpleUUID();
        MDC.put(TraceConstants.TRACE_ID, traceId);
        return traceId;
    }

    /**
     * 设置指定的 traceId
     */
    public static void setupTraceId(String traceId) {
        MDC.put(TraceConstants.TRACE_ID, traceId);
    }

    /**
     * 获取当前 traceId
     */
    public static String getTraceId() {
        return MDC.get(TraceConstants.TRACE_ID);
    }

    /**
     * 清理 traceId
     */
    public static void clearTraceId() {
        MDC.remove(TraceConstants.TRACE_ID);
    }
}
