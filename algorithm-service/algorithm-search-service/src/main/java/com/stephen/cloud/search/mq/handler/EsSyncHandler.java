package com.stephen.cloud.search.mq.handler;

import cn.hutool.json.JSONUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.stephen.cloud.api.search.constant.EsIndexConstant;
import com.stephen.cloud.api.search.model.entity.BaseEsDTO;
import com.stephen.cloud.api.search.model.entity.PostEsDTO;
import com.stephen.cloud.api.search.model.entity.UserEsDTO;
import com.stephen.cloud.api.search.model.entity.ChunkEsDTO;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqDedupeLock;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncDataTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.EsSyncMessage;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * ES 单条数据同步处理器
 * <p>
 * 该处理器负责将单条业务变更（增/删/改）实时同步至 Elasticsearch 索引。
 * 采用了声明式去重锁 {@link RabbitMqDedupeLock} 防范网络重连导致的重复索引。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
@RabbitMqDedupeLock(prefix = "mq:es:sync:single")
public class EsSyncHandler implements RabbitMqHandler<EsSyncMessage> {

    @Resource
    private ElasticsearchClient elasticsearchClient;

    /**
     * 数据类型与实体 DTO 的静态映射表
     */
    private static final Map<EsSyncDataTypeEnum, Class<?>> DATA_TYPE_CLASS_MAP = new HashMap<>();

    static {
        DATA_TYPE_CLASS_MAP.put(EsSyncDataTypeEnum.POST, PostEsDTO.class);
        DATA_TYPE_CLASS_MAP.put(EsSyncDataTypeEnum.USER, UserEsDTO.class);
        DATA_TYPE_CLASS_MAP.put(EsSyncDataTypeEnum.CHUNK, ChunkEsDTO.class);
    }

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

        Class<?> clazz = DATA_TYPE_CLASS_MAP.get(dataTypeEnum);
        String index = getIndexByType(dataTypeEnum);

        if (clazz == null || index == null) {
            log.warn("[EsSyncHandler] 处理器暂未支持该数据类型同步: {}", msg.getDataType());
            return;
        }

        handleSync(msg, index, clazz);
    }

    /**
     * 执行具体的 ES 同步操作。
     *
     * @param msg   解包后的同步消息体
     * @param index 目标索引名
     * @param clazz 目标 DTO 类型
     * @param <T>   泛型占位
     * @throws IOException ES 通讯异常
     */
    private <T> void handleSync(EsSyncMessage msg, String index, Class<T> clazz) throws IOException {
        String op = msg.getOperation();
        Long id = msg.getDataId();
        String content = msg.getDataContent();

        // 1. 删除操作直接物理移除 (或逻辑移除映射)
        if ("delete".equals(op)) {
            elasticsearchClient.delete(d -> d.index(index).id(String.valueOf(id)));
            log.info("[EsSyncHandler] ES 删除成功, index: {}, id: {}", index, id);
        }
        // 2. 更新或创建操作
        else if ("upsert".equals(op) || "create".equals(op)) {
            T dto = JSONUtil.toBean(content, clazz);
            // 兜底逻辑：如果 DTO 本身标记为逻辑删除，则从索引中移除
            if (dto instanceof BaseEsDTO baseEsDTO) {
                if (baseEsDTO.getIsDelete() != null && baseEsDTO.getIsDelete() == 1) {
                    elasticsearchClient.delete(d -> d.index(index).id(String.valueOf(id)));
                    log.info("[EsSyncHandler] 业务数据标记为已逻辑删除，同步执行 ES 物理删除: id={}", id);
                    return;
                }
            }
            elasticsearchClient.index(i -> i.index(index).id(String.valueOf(id)).document(dto));
            log.info("[EsSyncHandler] ES 同步索引成功: index={}, id={}", index, id);
        }
    }

    /**
     * 根据业务类型查找对应的 ES 索引名
     */
    private String getIndexByType(EsSyncDataTypeEnum type) {
        return switch (type) {
            case POST -> EsIndexConstant.POST_INDEX;
            case USER -> EsIndexConstant.USER_INDEX;
            case CHUNK -> EsIndexConstant.CHUNK_INDEX;
            default -> null;
        };
    }

    @Override
    public Class<EsSyncMessage> getDataType() {
        return EsSyncMessage.class;
    }
}
