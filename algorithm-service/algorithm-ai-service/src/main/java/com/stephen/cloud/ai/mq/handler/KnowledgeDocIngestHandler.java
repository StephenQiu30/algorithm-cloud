package com.stephen.cloud.ai.mq.handler;

import com.stephen.cloud.ai.service.KnowledgeIngestService;
import com.stephen.cloud.api.knowledge.model.dto.knowledgedocument.KnowledgeDocIngestMessage;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqDedupeLock;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RabbitMqDedupeLock(prefix = "mq:kb:ingest", expire = 86400)
public class KnowledgeDocIngestHandler implements RabbitMqHandler<KnowledgeDocIngestMessage> {

    @Resource
    private KnowledgeIngestService knowledgeIngestService;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.KNOWLEDGE_DOC_INGEST.getValue();
    }

    @Override
    public void onMessage(KnowledgeDocIngestMessage dto, RabbitMessage rabbitMessage) throws Exception {
        log.info("[KnowledgeDocIngestHandler] msgId={}, bizType={}, documentId={}",
                rabbitMessage != null ? rabbitMessage.getMsgId() : null,
                rabbitMessage != null ? rabbitMessage.getBizType() : null,
                dto != null ? dto.getDocumentId() : null);
        knowledgeIngestService.ingestDocument(dto);
    }

    @Override
    public Class<KnowledgeDocIngestMessage> getDataType() {
        return KnowledgeDocIngestMessage.class;
    }
}
