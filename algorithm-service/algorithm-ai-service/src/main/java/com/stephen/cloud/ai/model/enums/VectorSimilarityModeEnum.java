package com.stephen.cloud.ai.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 向量相似度检索模式（纯 kNN / 混合）。
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum VectorSimilarityModeEnum {

    KNN("knn"),
    HYBRID("hybrid");

    private final String value;
}
