package com.stephen.cloud.api.ai.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

/**
 * AI 工具类型枚举：用于管理 Tool Calling 中的函数名称。
 *
 * @author StephenQiu30
 */
@Getter
public enum AiToolEnum {

    /**
     * 算法知识库检索工具
     */
    ALGORITHM_KNOWLEDGE_SEARCH(AiToolEnum.ALGORITHM_KNOWLEDGE_SEARCH_VALUE, "算法知识库检索工具");

    /**
     * 常量值：供 @Bean 等注解使用
     */
    public static final String ALGORITHM_KNOWLEDGE_SEARCH_VALUE = "algorithmKnowledgeSearch";

    /**
     * 枚举值
     */
    private final String value;

    /**
     * 枚举文本
     */
    private final String text;

    AiToolEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 值
     * @return 枚举
     */
    public static AiToolEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (AiToolEnum aiToolEnum : AiToolEnum.values()) {
            if (aiToolEnum.value.equals(value)) {
                return aiToolEnum;
            }
        }
        return null;
    }
}
