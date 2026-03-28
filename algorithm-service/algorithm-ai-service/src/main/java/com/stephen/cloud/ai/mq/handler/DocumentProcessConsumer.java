package com.stephen.cloud.ai.mq.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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
import org.apache.commons.lang3.StringUtils;
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
            putIfNotNull(metadata, "knowledgeBaseId", message.getKnowledgeBaseId());
            putIfNotNull(metadata, "documentId", message.getDocumentId());
            putIfNotNull(metadata, "documentName", document.getName());
            putIfNotNull(metadata, "sourceType", document.getSourceType());
            putIfNotNull(metadata, "bizTag", document.getBizTag());
            putIfNotNull(metadata, "version", document.getVersion());
            enrichMetadataFromExtraMeta(document.getExtraMeta(), metadata);
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

    private void putIfNotNull(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }

    private void enrichMetadataFromExtraMeta(String extraMeta, Map<String, Object> metadata) {
        if (StringUtils.isBlank(extraMeta) || !JSONUtil.isTypeJSONObject(extraMeta)) {
            return;
        }
        JSONObject jsonObject = JSONUtil.parseObj(extraMeta);
        jsonObject.forEach((key, value) -> {
            if (StringUtils.isBlank(key) || value == null) {
                return;
            }
            if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
                metadata.putIfAbsent(key, value);
            }
        });
    }
}
