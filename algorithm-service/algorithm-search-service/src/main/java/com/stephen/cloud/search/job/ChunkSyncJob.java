package com.stephen.cloud.search.job;

import com.stephen.cloud.common.rabbitmq.enums.EsSyncDataTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.SyncCommandMessage;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 文档分片 ES 同步任务
 *
 * @author StephenQiu30
 */
@Component
@Slf4j
public class ChunkSyncJob {

    @Resource
    private RabbitMqSender rabbitMqSender;

    /**
     * 每 2 小时全量同步一次
     */
    @Scheduled(cron = "0 0 0/2 * * *")
    public void fullSync() {
        log.info("[ChunkSyncJob] 开始全量同步分片数据...");
        SyncCommandMessage message = SyncCommandMessage.builder()
                .dataType(EsSyncDataTypeEnum.CHUNK)
                .syncType(EsSyncTypeEnum.FULL)
                .timestamp(System.currentTimeMillis())
                .build();
        
        rabbitMqSender.send(MqBizTypeEnum.SYNC_COMMAND_CHUNK, 
                EsSyncDataTypeEnum.CHUNK.getValue() + ":full:" + System.currentTimeMillis(), 
                message);
        log.info("[ChunkSyncJob] 全量同步指令已发送");
    }

    /**
     * 每 10 分钟增量同步一次
     */
    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void incrementalSync() {
        log.info("[ChunkSyncJob] 开始增量同步分片数据...");
        // 查询最近 10 分钟内的数据
        long tenMinutesAgo = System.currentTimeMillis() - 10 * 60 * 1000;
        SyncCommandMessage message = SyncCommandMessage.builder()
                .dataType(EsSyncDataTypeEnum.CHUNK)
                .syncType(EsSyncTypeEnum.INC)
                .minUpdateTime(new Date(tenMinutesAgo))
                .timestamp(System.currentTimeMillis())
                .build();

        rabbitMqSender.send(MqBizTypeEnum.SYNC_COMMAND_CHUNK, 
                EsSyncDataTypeEnum.CHUNK.getValue() + ":incremental:" + System.currentTimeMillis(), 
                message);
        log.info("[ChunkSyncJob] 增量同步指令已发送, minUpdateTime: {}", message.getMinUpdateTime());
    }
}
