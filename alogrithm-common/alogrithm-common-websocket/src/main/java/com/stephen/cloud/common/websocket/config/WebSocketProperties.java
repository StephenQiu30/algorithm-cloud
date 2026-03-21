package com.stephen.cloud.common.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * WebSocket 配置属性
 *
 * @author StephenQiu30
 */
@Data
@Component
@ConfigurationProperties(prefix = "websocket")
public class WebSocketProperties {

    /**
     * WebSocket 端口
     */
    private Integer port = 9090;

    /**
     * Boss 线程数
     */
    private Integer bossThread = 1;

    /**
     * Worker 线程数
     */
    private Integer workerThread = 4;

    /**
     * WebSocket 路径
     */
    private String path = "/websocket";

}
