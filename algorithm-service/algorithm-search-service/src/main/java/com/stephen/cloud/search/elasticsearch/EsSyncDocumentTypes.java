package com.stephen.cloud.search.elasticsearch;

import com.stephen.cloud.api.search.constant.EsIndexConstant;
import com.stephen.cloud.api.search.model.entity.ChunkEsDTO;
import com.stephen.cloud.api.search.model.entity.PostEsDTO;
import com.stephen.cloud.api.search.model.entity.UserEsDTO;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncDataTypeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * ES 同步消息在消费端的 dataType 与索引名、文档 DTO 类型映射。
 *
 * @author StephenQiu30
 */
public final class EsSyncDocumentTypes {

    private static final Map<EsSyncDataTypeEnum, Class<?>> DATA_TYPE_CLASS_MAP = new HashMap<>();

    static {
        DATA_TYPE_CLASS_MAP.put(EsSyncDataTypeEnum.POST, PostEsDTO.class);
        DATA_TYPE_CLASS_MAP.put(EsSyncDataTypeEnum.USER, UserEsDTO.class);
        DATA_TYPE_CLASS_MAP.put(EsSyncDataTypeEnum.CHUNK, ChunkEsDTO.class);
    }

    private EsSyncDocumentTypes() {
    }

    public static String indexOf(EsSyncDataTypeEnum type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case POST -> EsIndexConstant.POST_INDEX;
            case USER -> EsIndexConstant.USER_INDEX;
            case CHUNK -> EsIndexConstant.CHUNK_INDEX;
        };
    }

    public static Class<?> classOf(EsSyncDataTypeEnum type) {
        return type == null ? null : DATA_TYPE_CLASS_MAP.get(type);
    }
}
