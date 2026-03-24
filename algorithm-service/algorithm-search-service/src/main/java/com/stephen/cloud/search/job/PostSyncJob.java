package com.stephen.cloud.search.job;

import com.stephen.cloud.common.rabbitmq.enums.EsSyncDataTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.SyncCommandMessage;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 帖子同步任务 (MQ 指令版)
 *
 * @author stephen
 */
@Slf4j
@Component
public class PostSyncJob {

    @Resource
    private RabbitMqSender mqSender;

    /**
     * 启动时执行一次全量同步
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("[PostSyncJob] 应用启动，执行一次全量同步帖子...");
        fullSync();
    }

    /**
     * 全量同步
     * 每天凌晨 2 点执行
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void fullSync() {
        log.info("[PostSyncJob] 发送全量同步帖子指令...");
        SyncCommandMessage message = SyncCommandMessage.builder()
                .dataType(EsSyncDataTypeEnum.POST)
                .syncType(EsSyncTypeEnum.FULL)
                .build();
        mqSender.send(MqBizTypeEnum.SYNC_COMMAND_POST, message);
    }

    /**
     * 增量同步
     * 每 5 分钟执行一次
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void incrementalSync() {
        log.info("[PostSyncJob] 发送增量同步帖子指令...");
        // 指令发送最近 5 分钟内更新的数据指令
        long fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000;
        SyncCommandMessage message = SyncCommandMessage.builder()
                .dataType(EsSyncDataTypeEnum.POST)
                .syncType(EsSyncTypeEnum.INC)
                .minUpdateTime(new Date(fiveMinutesAgo))
                .build();
        mqSender.send(MqBizTypeEnum.SYNC_COMMAND_POST, message);
    }
}
