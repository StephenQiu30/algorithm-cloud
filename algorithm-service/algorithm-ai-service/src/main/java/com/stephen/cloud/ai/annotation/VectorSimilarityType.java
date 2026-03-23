package com.stephen.cloud.ai.annotation;

import com.stephen.cloud.api.knowledge.model.enums.VectorSimilarityModeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记向量相似度策略实现对应的检索模式。
 *
 * @author StephenQiu30
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VectorSimilarityType {
    VectorSimilarityModeEnum value();
}
