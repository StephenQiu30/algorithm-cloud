package com.stephen.cloud.common.rabbitmq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Elasticsearch 批量同步消息
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EsSyncBatchMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据类型（post/user）
     */
    private String dataType;

    /**
     * 操作类型（upsert/delete）
     */
    private String operation;

    /**
     * 数据列表（JSON格式字符串列表）
     */
    private List<String> dataContentList;

    /**
     * 时间戳
     */
    private Long timestamp;
}
