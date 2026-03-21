package com.stephen.cloud.post.mq.handler;

import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqDedupeLock;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.SyncCommandMessage;
import com.stephen.cloud.post.service.PostService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 帖子同步指令处理器
 * <p>
 * 该处理器接收来自管理端或系统调度的同步指令，触发帖子数据从数据库向 Elasticsearch 的全量或增量同步。
 * 采用了 {@link RabbitMqDedupeLock} 进行指令级的去重，避免同一时间段内发起重复的同步任务导致数据库压力过大。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
@RabbitMqDedupeLock(prefix = "mq:sync:command:post")
public class PostSyncCommandHandler implements RabbitMqHandler<SyncCommandMessage> {

    @Resource
    private PostService postService;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.SYNC_COMMAND_POST.getValue();
    }

    /**
     * 处理同步指令。
     * <p>
     * 逻辑上委托给 {@link PostService#syncToEs} 执行，包含了全文索引的构建逻辑。
     * </p>
     */
    @Override
    public void onMessage(SyncCommandMessage msg, RabbitMessage rabbitMessage) throws Exception {
        log.info("[PostSyncCommandHandler] 接收到同步指令: dataType={}, syncType={}",
                msg.getDataType(), msg.getSyncType());

        postService.syncToEs(msg.getSyncType(), msg.getMinUpdateTime());

        log.info("[PostSyncCommandHandler] 同步任务触发成功");
    }

    @Override
    public Class<SyncCommandMessage> getDataType() {
        return SyncCommandMessage.class;
    }
}
