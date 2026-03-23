package com.stephen.cloud.api.ai.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

/**
 * AI 模型类型枚举
 *
 * @author StephenQiu30
 */
@Getter
public enum AiModelTypeEnum {

    /**
     * 通义千问 (阿里云 DashScope)
     */
    DASHSCOPE("dashscope", "通义千问"),

    /**
     * RAG (检索增强生成)
     */
    RAG("rag", "RAG"),

    /**
     * AGENTIC RAG (代理式 RAG)
     */
    AGENTIC_RAG("agentic_rag", "代理式 RAG");

    /**
     * 枚举值
     */
    private final String value;

    /**
     * 枚举文本
     */
    private final String text;

    AiModelTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 值
     * @return 枚举
     */
    public static AiModelTypeEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (AiModelTypeEnum aiModelTypeEnum : AiModelTypeEnum.values()) {
            if (aiModelTypeEnum.value.equals(value)) {
                return aiModelTypeEnum;
            }
        }
        return null;
    }
}
