package com.stephen.cloud.ai.mq;

import com.stephen.cloud.api.knowledge.model.dto.knowledgedocument.KnowledgeDocIngestMessage;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KnowledgeIngestMqProducer {

    @Resource
    private RabbitMqSender mqSender;

    public void sendIngestCreated(KnowledgeDocIngestMessage message) {
        if (message == null || message.getDocumentId() == null) {
            log.warn("[KnowledgeIngestMqProducer] 入库消息为空或文档ID为空，跳过发送");
            return;
        }
        try {
            String bizId = "knowledge_ingest_" + message.getDocumentId();
            mqSender.send(MqBizTypeEnum.KNOWLEDGE_DOC_INGEST, bizId, message);
            log.info("[KnowledgeIngestMqProducer] 发送文档入库事件成功, docId: {}, kbId: {}",
                    message.getDocumentId(), message.getKnowledgeBaseId());
        } catch (Exception e) {
            log.error("[KnowledgeIngestMqProducer] 发送文档入库事件失败, docId: {}, kbId: {}",
                    message.getDocumentId(), message.getKnowledgeBaseId(), e);
        }
    }
}
