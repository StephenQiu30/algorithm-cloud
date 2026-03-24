package com.stephen.cloud.ai.mq.handler;

import com.stephen.cloud.ai.knowledge.etl.DocumentETLPipeline;
import com.stephen.cloud.ai.model.entity.Document;
import com.stephen.cloud.ai.mq.model.DocumentProcessMessage;
import com.stephen.cloud.ai.service.DocumentService;
import com.stephen.cloud.api.ai.model.enums.DocumentParseStatusEnum;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqDedupeLock;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RabbitMqDedupeLock(prefix = "mq:knowledge:doc", expire = 86400)
public class DocumentProcessConsumer implements RabbitMqHandler<DocumentProcessMessage> {

    @Resource
    private DocumentService documentService;

    @Resource
    private DocumentETLPipeline documentETLPipeline;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.KNOWLEDGE_DOC_INGEST.getValue();
    }

    @Override
    public void onMessage(DocumentProcessMessage message, RabbitMessage rabbitMessage) throws Exception {
        String msgId = rabbitMessage == null ? null : rabbitMessage.getMsgId();
        Long documentId = message.getDocumentId();
        log.info("[DocumentProcessConsumer] 收到文档处理消息, msgId={}, documentId={}", msgId, documentId);
        Document document = documentService.getById(documentId);
        if (document == null) {
            log.warn("[DocumentProcessConsumer] 文档不存在，忽略处理, msgId={}, documentId={}", msgId, documentId);
            return;
        }
        Date now = new Date();
        document.setStatus(DocumentParseStatusEnum.PROCESSING.getValue());
        document.setProcessStartTime(now);
        documentService.updateById(document);
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("knowledgeBaseId", message.getKnowledgeBaseId());
            metadata.put("documentId", message.getDocumentId());
            metadata.put("documentName", document.getName());
            int chunkCount = documentETLPipeline.process(message.getFilePath(), message.getFileExtension(), metadata);
            document.setStatus(DocumentParseStatusEnum.COMPLETED.getValue());
            document.setChunkCount(chunkCount);
            document.setErrorMessage(null);
            document.setProcessEndTime(new Date());
            documentService.updateById(document);
            log.info("[DocumentProcessConsumer] 文档处理成功, msgId={}, documentId={}, cost={}ms",
                    msgId, documentId, System.currentTimeMillis() - start);
        } catch (Exception e) {
            document.setStatus(DocumentParseStatusEnum.FAILED.getValue());
            document.setErrorMessage(e.getMessage());
            document.setProcessEndTime(new Date());
            documentService.updateById(document);
            log.error("[DocumentProcessConsumer] 文档处理失败, msgId={}, documentId={}", msgId, documentId, e);
            throw e;
        }
    }

    @Override
    public Class<DocumentProcessMessage> getDataType() {
        return DocumentProcessMessage.class;
    }
}
