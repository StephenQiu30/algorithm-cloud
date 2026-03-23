package com.stephen.cloud.ai.vector;

import com.stephen.cloud.ai.annotation.VectorSimilarityType;
import com.stephen.cloud.api.knowledge.model.enums.VectorSimilarityModeEnum;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向量相似度策略注册表。
 *
 * @author StephenQiu30
 */
@Component
@Slf4j
public class VectorSimilarityStrategyRegistry {

    @Resource
    private List<VectorSimilaritySearchStrategy> vectorSimilaritySearchStrategies;

    private final Map<VectorSimilarityModeEnum, VectorSimilaritySearchStrategy> strategyMap =
            new ConcurrentHashMap<>();

    @PostConstruct
    public void doInit() {
        log.info("开始注册向量相似度策略...");
        for (VectorSimilaritySearchStrategy strategy : vectorSimilaritySearchStrategies) {
            Class<?> targetClass = AopUtils.getTargetClass(strategy);
            VectorSimilarityType ann = targetClass.getAnnotation(VectorSimilarityType.class);
            if (ann != null) {
                strategyMap.put(ann.value(), strategy);
            } else {
                log.warn("未为策略 {} 标注 @VectorSimilarityType", targetClass.getSimpleName());
            }
        }
        log.info("已注册向量相似度策略: {}", strategyMap.keySet());
    }

    public VectorSimilaritySearchStrategy getStrategy(VectorSimilarityModeEnum mode) {
        VectorSimilaritySearchStrategy strategy = strategyMap.get(mode);
        if (strategy == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未找到指定类型的向量相似度策略: " + mode);
        }
        return strategy;
    }
}
