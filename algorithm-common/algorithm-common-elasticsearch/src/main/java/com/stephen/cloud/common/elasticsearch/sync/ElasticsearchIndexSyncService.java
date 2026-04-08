package com.stephen.cloud.common.elasticsearch.sync;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Elasticsearch 索引同步服务
 * <p>
 * 提供 ES 索引的单条和批量同步能力，支持创建、更新、删除操作。
 * 自动处理逻辑删除标记的同步为物理删除。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Service
@ConditionalOnBean(ElasticsearchClient.class)
public class ElasticsearchIndexSyncService {

    private static final int BULK_BATCH_SIZE = 500;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Resource
    private ElasticsearchClient elasticsearchClient;

    public void syncSingle(String index, Class<?> clazz, String operation, Long dataId, String dataContent)
            throws IOException {
        String op = normalizeOperation(operation);

        if ("delete".equals(op)) {
            elasticsearchClient.delete(d -> d.index(index).id(String.valueOf(dataId)));
            log.info("[EsIndexSync] ES 删除成功, index: {}, id: {}", index, dataId);
            return;
        }
        if (!("upsert".equals(op) || "create".equals(op))) {
            log.debug("[EsIndexSync] 忽略的操作类型: {}", op);
            return;
        }

        JsonNode root = objectMapper.readTree(dataContent);
        if (isMarkedDeleted(root)) {
            elasticsearchClient.delete(d -> d.index(index).id(String.valueOf(dataId)));
            log.info("[EsIndexSync] 业务数据标记为已逻辑删除，同步执行 ES 物理删除: id={}", dataId);
            return;
        }
        Object dto = objectMapper.treeToValue(root, clazz);
        elasticsearchClient.index(i -> i.index(index).id(String.valueOf(dataId)).document(dto));
        log.info("[EsIndexSync] ES 同步索引成功: index={}, id={}", index, dataId);
    }

    public void syncBatch(String index, Class<?> clazz, String operation, List<String> dataContentList)
            throws IOException {
        if (dataContentList == null || dataContentList.isEmpty()) {
            log.debug("[EsIndexSync] 批量同步数据项为空，跳过");
            return;
        }
        boolean isDelete = "delete".equals(normalizeOperation(operation));
        int batchSize = Math.max(1, BULK_BATCH_SIZE);

        BulkRequest.Builder br = newBulkRequestBuilder();
        int inChunk = 0;
        long totalLines = dataContentList.size();

        for (String content : dataContentList) {
            boolean added = appendBulkLine(br, index, clazz, content, isDelete);
            if (!added) {
                continue;
            }
            inChunk++;
            if (inChunk >= batchSize) {
                flushBulk(br, index, inChunk);
                br = newBulkRequestBuilder();
                inChunk = 0;
            }
        }
        if (inChunk > 0) {
            flushBulk(br, index, inChunk);
        }
        log.info("[EsIndexSync] 批量同步处理完成: index={}, lines={}", index, totalLines);
    }

    private BulkRequest.Builder newBulkRequestBuilder() {
        return new BulkRequest.Builder().refresh(Refresh.False);
    }

    private boolean appendBulkLine(BulkRequest.Builder br, String index, Class<?> clazz, String content,
            boolean isDelete) throws IOException {
        JsonNode root = objectMapper.readTree(content);
        JsonNode idNode = root.get("id");
        if (idNode == null || idNode.isNull()) {
            return false;
        }
        long idLong = idNode.isNumber() ? idNode.longValue() : Long.parseLong(idNode.asText());
        String idStr = String.valueOf(idLong);

        if (isDelete) {
            br.operations(op -> op.delete(d -> d.index(index).id(idStr)));
            return true;
        }

        if (isMarkedDeleted(root)) {
            br.operations(op -> op.delete(d -> d.index(index).id(idStr)));
            return true;
        }

        JavaType javaType = objectMapper.getTypeFactory().constructType(clazz);
        Object dto = objectMapper.readValue(content, javaType);
        br.operations(op -> op.index(i -> i.index(index).id(idStr).document(dto)));
        return true;
    }

    private static boolean isMarkedDeleted(JsonNode root) {
        JsonNode n = root.get("isDelete");
        if (n == null || n.isNull()) {
            return false;
        }
        if (n.isNumber()) {
            return n.intValue() == 1;
        }
        if (n.isTextual()) {
            return "1".equals(n.asText());
        }
        return false;
    }

    private void flushBulk(BulkRequest.Builder br, String index, int opCount) throws IOException {
        BulkRequest request = br.build();
        if (request.operations() == null || request.operations().isEmpty()) {
            return;
        }
        BulkResponse result = elasticsearchClient.bulk(request);
        if (result.errors()) {
            log.error("[EsIndexSync] 批量同步存在失败项, index={}", index);
            logBulkFailures(result);
        } else {
            log.info("[EsIndexSync] 批量同步子批次成功: index={}, ops={}", index, opCount);
        }
    }

    private void logBulkFailures(BulkResponse result) {
        for (BulkResponseItem item : result.items()) {
            if (item.error() != null) {
                log.error("[EsIndexSync] 失败项: id={}, reason={}", item.id(), item.error().reason());
            } else if (item.status() >= 400) {
                log.error("[EsIndexSync] 失败项: id={}, status={}", item.id(), item.status());
            }
        }
    }

    private static String normalizeOperation(String operation) {
        if (operation == null) {
            return "";
        }
        return operation.trim().toLowerCase();
    }
}
