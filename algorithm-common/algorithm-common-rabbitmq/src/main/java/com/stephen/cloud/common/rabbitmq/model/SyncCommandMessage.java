package com.stephen.cloud.common.rabbitmq.model;

import com.stephen.cloud.common.rabbitmq.enums.EsSyncDataTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 数据同步指令消息模型
 * <p>
 * 该消息用于在微服务架构中分发全局同步指令（如全量重索引或增量修复）。
 * 包含了数据类型标识、同步模式以及时间窗口元数据。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncCommandMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 同步目标数据类型 (如: post, user, notification 等)
     */
    private EsSyncDataTypeEnum dataType;

    /**
     * 同步模式标识 (FULL: 全量同步, INC: 增量同步)
     */
    private EsSyncTypeEnum syncType;

    /**
     * 增量同步的起始起始时间。
     * <p>
     * 仅在 {@link EsSyncTypeEnum#INC} 模式下生效。
     * </p>
     */
    private Date minUpdateTime;

    /**
     * 消息产生的时间戳 (毫秒)
     */
    @Builder.Default
    private Long timestamp = System.currentTimeMillis();
}
