package com.stephen.cloud.ai.mq;

import com.stephen.cloud.ai.mq.model.DocumentProcessMessage;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文档处理消息生产者
 * <p>
 * 负责将文档处理任务发送到 MQ 队列，触发异步 ETL 流程
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class DocumentProcessProducer {

    @Resource
    private RabbitMqSender mqSender;

    public void sendMessage(DocumentProcessMessage message) {
        if (message == null || message.getDocumentId() == null) {
            log.warn("[DocumentProcessProducer] 消息体或 documentId 为空，跳过发送");
            return;
        }
        try {
            String bizId = "knowledge_doc:" + message.getDocumentId();
            mqSender.send(MqBizTypeEnum.KNOWLEDGE_DOC_INGEST, bizId, message);
            log.info("[DocumentProcessProducer] 发送文档处理消息成功, documentId={}, bizId={}",
                    message.getDocumentId(), bizId);
        } catch (Exception e) {
            log.error("[DocumentProcessProducer] 发送文档处理消息失败, documentId={}", message.getDocumentId(), e);
        }
    }
}
