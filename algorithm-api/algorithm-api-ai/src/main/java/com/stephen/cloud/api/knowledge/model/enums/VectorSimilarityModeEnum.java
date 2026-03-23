package com.stephen.cloud.api.knowledge.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum VectorSimilarityModeEnum {
    KNN("纯向量 kNN", "knn"),
    HYBRID("混合检索 Hybrid", "hybrid");

    private final String text;
    private final String value;

    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public static VectorSimilarityModeEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (VectorSimilarityModeEnum anEnum : VectorSimilarityModeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

