package com.stephen.cloud.api.ai.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 检索策略枚举
 *
 * @author StephenQiu30
 */
@Getter
public enum RetrievalStrategyEnum {

    /**
     * 双路召回 + RRF
     */
    HYBRID_RRF("HYBRID_RRF", "双路召回+RRF"),

    /**
     * 双路召回 + RRF + 重排
     */
    HYBRID_RRF_RERANK("HYBRID_RRF_RERANK", "双路召回+RRF+重排"),

    /**
     * 知识库不足时触发联网搜索兜底
     */
    WEB_SEARCH_FALLBACK("WEB_SEARCH_FALLBACK", "联网搜索兜底");

    /**
     * 枚举值
     */
    private final String value;

    /**
     * 枚举文本
     */
    private final String text;

    RetrievalStrategyEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 值
     * @return 枚举
     */
    public static RetrievalStrategyEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (RetrievalStrategyEnum retrievalStrategyEnum : RetrievalStrategyEnum.values()) {
            if (retrievalStrategyEnum.value.equals(value)) {
                return retrievalStrategyEnum;
            }
        }
        return null;
    }
}
