package com.stephen.cloud.common.rabbitmq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Elasticsearch 同步消息
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EsSyncMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据类型（post/user）
     */
    private String dataType;

    /**
     * 操作类型（create/update/delete）
     */
    private String operation;

    /**
     * 数据 ID
     */
    private Long dataId;

    /**
     * 数据内容（JSON格式）
     */
    private String dataContent;

    /**
     * 时间戳
     */
    private Long timestamp;
}
