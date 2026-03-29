package com.stephen.cloud.search.mq.handler;

import com.stephen.cloud.common.elasticsearch.sync.ElasticsearchIndexSyncService;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqDedupeLock;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncDataTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.EsSyncBatchMessage;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.search.elasticsearch.EsSyncDocumentTypes;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RabbitMqDedupeLock(prefix = "mq:es:sync:batch")
public class EsSyncBatchHandler implements RabbitMqHandler<EsSyncBatchMessage> {

    @Resource
    private ElasticsearchIndexSyncService elasticsearchIndexSyncService;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.ES_SYNC_BATCH.getValue();
    }

    @Override
    public void onMessage(EsSyncBatchMessage msg, RabbitMessage rabbitMessage) throws Exception {
        EsSyncDataTypeEnum dataTypeEnum = EsSyncDataTypeEnum.getEnumByValue(msg.getDataType());
        if (dataTypeEnum == null) {
            log.warn("[EsSyncBatchHandler] 收到未定义的数据类型: {}", msg.getDataType());
            return;
        }
        Class<?> clazz = EsSyncDocumentTypes.classOf(dataTypeEnum);
        String index = EsSyncDocumentTypes.indexOf(dataTypeEnum);
        if (clazz == null || index == null) {
            log.warn("[EsSyncBatchHandler] 处理器暂未支持该数据类型同步: {}", msg.getDataType());
            return;
        }
        elasticsearchIndexSyncService.syncBatch(index, clazz, msg.getOperation(), msg.getDataContentList());
    }

    @Override
    public Class<EsSyncBatchMessage> getDataType() {
        return EsSyncBatchMessage.class;
    }
}
