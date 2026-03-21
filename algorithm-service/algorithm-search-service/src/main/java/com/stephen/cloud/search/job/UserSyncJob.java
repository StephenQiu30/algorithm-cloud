package com.stephen.cloud.search.job;

import com.stephen.cloud.common.rabbitmq.enums.EsSyncDataTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.SyncCommandMessage;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 用户同步任务 (MQ 指令版)
 *
 * @author stephen
 */
@Slf4j
@Component
public class UserSyncJob {

    @Resource
    private RabbitMqSender mqSender;

    /**
     * 启动时执行一次全量同步
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("[UserSyncJob] 应用启动，执行一次全量同步用户...");
        fullSync();
    }

    /**
     * 全量同步
     * 每天凌晨 3 点执行
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void fullSync() {
        log.info("[UserSyncJob] 发送全量同步用户指令...");
        SyncCommandMessage message = SyncCommandMessage.builder()
                .dataType(EsSyncDataTypeEnum.USER)
                .syncType(EsSyncTypeEnum.FULL)
                .build();
        mqSender.send(MqBizTypeEnum.SYNC_COMMAND_USER, message);
    }

    /**
     * 增量同步
     * 每 5 分钟执行一次
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void incrementalSync() {
        log.info("[UserSyncJob] 发送增量同步用户指令...");
        long fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000;
        SyncCommandMessage message = SyncCommandMessage.builder()
                .dataType(EsSyncDataTypeEnum.USER)
                .syncType(EsSyncTypeEnum.INC)
                .minUpdateTime(new Date(fiveMinutesAgo))
                .build();
        mqSender.send(MqBizTypeEnum.SYNC_COMMAND_USER, message);
    }
}
