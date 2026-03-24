package com.stephen.cloud.api.ai.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 命中原因枚举
 *
 * @author StephenQiu30
 */
@Getter
public enum MatchReasonEnum {

    /**
     * 重排命中
     */
    RERANK("rerank", "重排命中"),

    /**
     * 关键词强约束命中
     */
    MUST_TERM("mustTerm", "关键词强约束命中"),

    /**
     * 混合召回命中
     */
    HYBRID("hybrid", "混合召回命中");

    /**
     * 枚举值
     */
    private final String value;

    /**
     * 枚举文本
     */
    private final String text;

    MatchReasonEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 值
     * @return 枚举
     */
    public static MatchReasonEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (MatchReasonEnum matchReasonEnum : MatchReasonEnum.values()) {
            if (matchReasonEnum.value.equals(value)) {
                return matchReasonEnum;
            }
        }
        return null;
    }
}
