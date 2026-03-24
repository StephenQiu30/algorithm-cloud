package com.stephen.cloud.api.knowledge.model.enums;

import lombok.Getter;

/**
 * RAG 指标标签枚举：统一管理 Micrometer 指标的 tag 值。
 *
 * @author StephenQiu30
 */
@Getter
public enum RagMetricTagEnum {

    // 检索模式标签
    MODE_TAG_KEY("mode"),

    // RAG 调用模式标签值
    CALL_MODE_SYNC("sync"),
    CALL_MODE_STREAM("stream");

    private final String value;

    RagMetricTagEnum(String value) {
        this.value = value;
    }
}
