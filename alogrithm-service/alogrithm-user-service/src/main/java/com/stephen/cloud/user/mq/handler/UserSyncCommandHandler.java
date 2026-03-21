package com.stephen.cloud.user.mq.handler;

import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqDedupeLock;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.SyncCommandMessage;
import com.stephen.cloud.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户同步指令处理器
 * <p>
 * 该处理器响应用户相关数据的同步指令，确保用户信息在 Elasticsearch 搜索引擎中的实时性与准确性。
 * 集成了声明式幂等锁，防范并发或重试场景下产生的冗余同步任务。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
@RabbitMqDedupeLock(prefix = "mq:sync:command:user")
public class UserSyncCommandHandler implements RabbitMqHandler<SyncCommandMessage> {

    @Resource
    private UserService userService;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.SYNC_COMMAND_USER.getValue();
    }

    /**
     * 执行同步业务逻辑。
     * <p>
     * 自动处理 {@link SyncCommandMessage} 中的全量或增量标识，并调用业务 Service。
     * </p>
     */
    @Override
    public void onMessage(SyncCommandMessage msg, RabbitMessage rabbitMessage) throws Exception {
        log.info("[UserSyncCommandHandler] 接收到同步指令: dataType={}, syncType={}",
                msg.getDataType(), msg.getSyncType());

        userService.syncToEs(msg.getSyncType(), msg.getMinUpdateTime());

        log.info("[UserSyncCommandHandler] 用户同步任务处理成功");
    }

    @Override
    public Class<SyncCommandMessage> getDataType() {
        return SyncCommandMessage.class;
    }
}
