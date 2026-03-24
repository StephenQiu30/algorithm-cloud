package com.stephen.cloud.ai.mq.handler;

import com.stephen.cloud.ai.service.ChunkService;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqDedupeLock;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.SyncCommandMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文档分片同步指令处理器
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
@RabbitMqDedupeLock(prefix = "mq:sync:command:chunk")
public class ChunkSyncCommandHandler implements RabbitMqHandler<SyncCommandMessage> {

    @Resource
    private ChunkService chunkService;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.SYNC_COMMAND_CHUNK.getValue();
    }

    @Override
    public void onMessage(SyncCommandMessage msg, RabbitMessage rabbitMessage) throws Exception {
        log.info("[ChunkSyncCommandHandler] 接收到同步指令: dataType={}, syncType={}",
                msg.getDataType(), msg.getSyncType());

        chunkService.syncToEs(msg.getSyncType(), msg.getMinUpdateTime());

        log.info("[ChunkSyncCommandHandler] 同步任务触发成功");
    }

    @Override
    public Class<SyncCommandMessage> getDataType() {
        return SyncCommandMessage.class;
    }
}
