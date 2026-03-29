package com.stephen.cloud.search.mq.handler;

import com.stephen.cloud.common.elasticsearch.sync.ElasticsearchIndexSyncService;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqDedupeLock;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncDataTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.EsSyncMessage;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.search.elasticsearch.EsSyncDocumentTypes;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RabbitMqDedupeLock(prefix = "mq:es:sync:single")
public class EsSyncHandler implements RabbitMqHandler<EsSyncMessage> {

    @Resource
    private ElasticsearchIndexSyncService elasticsearchIndexSyncService;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.ES_SYNC_SINGLE.getValue();
    }

    @Override
    public void onMessage(EsSyncMessage msg, RabbitMessage rabbitMessage) throws Exception {
        EsSyncDataTypeEnum dataTypeEnum = EsSyncDataTypeEnum.getEnumByValue(msg.getDataType());
        if (dataTypeEnum == null) {
            log.warn("[EsSyncHandler] 收到未定义的数据解析类型: {}", msg.getDataType());
            return;
        }
        Class<?> clazz = EsSyncDocumentTypes.classOf(dataTypeEnum);
        String index = EsSyncDocumentTypes.indexOf(dataTypeEnum);
        if (clazz == null || index == null) {
            log.warn("[EsSyncHandler] 处理器暂未支持该数据类型同步: {}", msg.getDataType());
            return;
        }
        elasticsearchIndexSyncService.syncSingle(index, clazz, msg.getOperation(), msg.getDataId(), msg.getDataContent());
    }

    @Override
    public Class<EsSyncMessage> getDataType() {
        return EsSyncMessage.class;
    }
}
