package com.stephen.cloud.ai.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识文档解析状态枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum KnowledgeParseStatusEnum {

    PENDING("待解析", 0),
    PROCESSING("解析中", 1),
    DONE("解析成功", 2),
    FAILED("解析失败", 3);

    private final String text;

    private final int value;

    /**
     * 获取值列表
     *
     * @return {@link List<Integer>}
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value value
     * @return {@link KnowledgeParseStatusEnum}
     */
    public static KnowledgeParseStatusEnum getEnumByValue(Integer value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (KnowledgeParseStatusEnum anEnum : KnowledgeParseStatusEnum.values()) {
            if (anEnum.value == value) {
                return anEnum;
            }
        }
        return null;
    }

}
