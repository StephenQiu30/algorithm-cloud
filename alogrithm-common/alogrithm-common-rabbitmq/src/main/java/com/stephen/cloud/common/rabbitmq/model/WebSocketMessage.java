package com.stephen.cloud.common.rabbitmq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * WebSocket 消息模型
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型
     */
    private Integer type;

    /**
     * 消息数据
     */
    private Object data;

    /**
     * 目标用户ID（单用户推送）
     */
    private Long userId;

    /**
     * 目标用户ID列表（多用户推送）
     */
    private List<Long> userIds;

    /**
     * 推送类型：single-单用户，multiple-多用户，broadcast-广播
     */
    private String pushType;

    /**
     * 业务类型（用于日志和监控）
     */
    private String bizType;

    /**
     * 业务ID（用于幂等性控制）
     */
    private String bizId;

    /**
     * 认证 Token（用于 WebSocket 连接认证）
     */
    private String token;
}
