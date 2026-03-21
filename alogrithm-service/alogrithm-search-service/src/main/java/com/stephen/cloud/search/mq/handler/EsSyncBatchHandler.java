package com.stephen.cloud.search.mq.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.stephen.cloud.api.search.constant.EsIndexConstant;
import com.stephen.cloud.api.search.model.entity.BaseEsDTO;
import com.stephen.cloud.api.search.model.entity.PostEsDTO;
import com.stephen.cloud.api.search.model.entity.UserEsDTO;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqDedupeLock;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncDataTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.EsSyncBatchMessage;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ES 批量数据同步处理器
 * <p>
 * 针对大规模业务变更（如全量重索引），利用 Elasticsearch Bulk API 开启批处理优化。
 * 通过 {@link RabbitMqDedupeLock} 锁定批次 MsgId，防范网络抖动在重试期间产生的重复写入压力。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
@RabbitMqDedupeLock(prefix = "mq:es:sync:batch")
public class EsSyncBatchHandler implements RabbitMqHandler<EsSyncBatchMessage> {

    @Resource
    private ElasticsearchClient elasticsearchClient;

    /**
     * 数据类型与实体 DTO 的静态映射表
     */
    private static final Map<EsSyncDataTypeEnum, Class<?>> DATA_TYPE_CLASS_MAP = new HashMap<>();

    static {
        DATA_TYPE_CLASS_MAP.put(EsSyncDataTypeEnum.POST, PostEsDTO.class);
        DATA_TYPE_CLASS_MAP.put(EsSyncDataTypeEnum.USER, UserEsDTO.class);
    }

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

        Class<?> clazz = DATA_TYPE_CLASS_MAP.get(dataTypeEnum);
        String index = getIndexByType(dataTypeEnum);

        if (clazz == null || index == null) {
            log.warn("[EsSyncBatchHandler] 处理器暂未支持该数据类型同步: {}", msg.getDataType());
            return;
        }

        List<String> dataList = msg.getDataContentList();
        if (CollUtil.isEmpty(dataList)) {
            log.debug("[EsSyncBatchHandler] 批量同步数据项为空，跳过");
            return;
        }

        processBatchSync(index, clazz, msg.getOperation(), dataList);
    }

    /**
     * 构建并执行 Bulk 请求逻辑。
     *
     * @param index     索引名称
     * @param clazz     DTO 类型
     * @param operation 操作类型 (delete/upsert/create)
     * @param dataList  待同步的数据 JSON 列表
     * @throws IOException ES 传输异常
     */
    private void processBatchSync(String index, Class<?> clazz, String operation, List<String> dataList)
            throws IOException {
        boolean isDelete = "delete".equals(operation);
        BulkRequest.Builder br = new BulkRequest.Builder();

        for (String content : dataList) {
            JSONObject jsonObject = JSONUtil.parseObj(content);
            Long id = jsonObject.getLong("id");
            if (id == null)
                continue;

            if (isDelete) {
                br.operations(op -> op.delete(d -> d.index(index).id(String.valueOf(id))));
            } else {
                Object dto = JSONUtil.toBean(content, clazz);
                // 物理删除兜底：逻辑删除的数据不应停留在索引中
                if (dto instanceof BaseEsDTO baseEsDTO && Integer.valueOf(1).equals(baseEsDTO.getIsDelete())) {
                    br.operations(op -> op.delete(d -> d.index(index).id(String.valueOf(id))));
                } else {
                    br.operations(op -> op.index(i -> i.index(index).id(String.valueOf(id)).document(dto)));
                }
            }
        }

        BulkResponse result = elasticsearchClient.bulk(br.build());
        if (result.errors()) {
            log.error("[EsSyncBatchHandler] 批量同步执行完毕，但存在失败项");
            for (BulkResponseItem item : result.items()) {
                if (item.error() != null) {
                    log.error("[EsSyncBatchHandler] 失败项: id={}, reason={}", item.id(), item.error().reason());
                } else if (item.status() >= 400) {
                    log.error("[EsSyncBatchHandler] 失败项: id={}, status={}", item.id(), item.status());
                }
            }
        } else {
            log.info("[EsSyncBatchHandler] 批量同步执行成功: index={}, count={}", index, dataList.size());
        }
    }

    /**
     * 索引名称动态路由转发
     */
    private String getIndexByType(EsSyncDataTypeEnum type) {
        return switch (type) {
            case POST -> EsIndexConstant.POST_INDEX;
            case USER -> EsIndexConstant.USER_INDEX;
            default -> null;
        };
    }

    @Override
    public Class<EsSyncBatchMessage> getDataType() {
        return EsSyncBatchMessage.class;
    }
}
