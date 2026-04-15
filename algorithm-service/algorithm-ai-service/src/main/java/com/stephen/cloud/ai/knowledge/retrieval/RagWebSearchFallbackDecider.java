package com.stephen.cloud.ai.knowledge.retrieval;

import com.stephen.cloud.ai.config.RagWebSearchProperties;
import com.stephen.cloud.ai.knowledge.retrieval.model.RetrievalResult;
import com.stephen.cloud.ai.knowledge.retrieval.model.WebSearchFallbackDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 联网搜索兜底判定器
 * <p>
 * 当前采用保守策略：
 * 1. 空召回时直接触发；
 * 2. 命中数不足且向量相关性显著偏低时触发；
 * 3. 对命中数不足且仅有关键词弱召回的情况，触发联网搜索补强。
 * </p>
 *
 * @author StephenQiu30
 */
@Component
@RequiredArgsConstructor
public class RagWebSearchFallbackDecider {

    private final RagWebSearchProperties ragWebSearchProperties;

    private final RagDocumentHelper ragDocumentHelper;

    public WebSearchFallbackDecision decide(RetrievalResult result, boolean requestEnabled) {
        List<Document> docs = result == null || result.getDocs() == null ? List.of() : result.getDocs();
        int hitCount = docs.size();
        List<Double> vectorSimilarities = collectVectorSimilarities(docs);
        Double topVectorSimilarity = vectorSimilarities.stream().max(Double::compareTo).orElse(null);
        Double averageVectorSimilarity = vectorSimilarities.isEmpty()
                ? null
                : vectorSimilarities.stream().mapToDouble(Double::doubleValue).average().orElse(0D);

        if (!requestEnabled) {
            return new WebSearchFallbackDecision(false, WebSearchFallbackDecision.REQUEST_DISABLED,
                    hitCount, topVectorSimilarity, averageVectorSimilarity);
        }
        if (!ragWebSearchProperties.isEnabled()) {
            return new WebSearchFallbackDecision(false, WebSearchFallbackDecision.GLOBAL_DISABLED,
                    hitCount, topVectorSimilarity, averageVectorSimilarity);
        }
        if (docs.isEmpty()) {
            return new WebSearchFallbackDecision(ragWebSearchProperties.isFallbackOnEmpty(),
                    WebSearchFallbackDecision.EMPTY_RECALL, hitCount, topVectorSimilarity, averageVectorSimilarity);
        }

        int minKnowledgeHits = Math.max(1, ragWebSearchProperties.getMinKnowledgeHits());
        if (hitCount >= minKnowledgeHits) {
            return new WebSearchFallbackDecision(false, WebSearchFallbackDecision.ENOUGH_CONTEXT,
                    hitCount, topVectorSimilarity, averageVectorSimilarity);
        }
        if (!ragWebSearchProperties.isFallbackOnLowConfidence()) {
            return new WebSearchFallbackDecision(false, WebSearchFallbackDecision.ENOUGH_CONTEXT,
                    hitCount, topVectorSimilarity, averageVectorSimilarity);
        }
        if (topVectorSimilarity == null) {
            return new WebSearchFallbackDecision(true, WebSearchFallbackDecision.KEYWORD_ONLY_LOW_COVERAGE,
                    hitCount, null, averageVectorSimilarity);
        }

        double minTopSimilarity = normalizeThreshold(ragWebSearchProperties.getMinTopSimilarity(), 0.72D);
        double minAverageSimilarity = normalizeThreshold(ragWebSearchProperties.getMinAverageSimilarity(), 0.65D);
        boolean lowTopSimilarity = topVectorSimilarity < minTopSimilarity;
        boolean lowAverageSimilarity = averageVectorSimilarity == null || averageVectorSimilarity < minAverageSimilarity;
        boolean shouldFallback = lowTopSimilarity && lowAverageSimilarity;

        return new WebSearchFallbackDecision(shouldFallback,
                shouldFallback ? WebSearchFallbackDecision.LOW_CONFIDENCE_RECALL
                        : WebSearchFallbackDecision.ENOUGH_CONTEXT,
                hitCount, topVectorSimilarity, averageVectorSimilarity);
    }

    private List<Double> collectVectorSimilarities(List<Document> docs) {
        List<Double> vectorSimilarities = new ArrayList<>();
        for (Document doc : docs) {
            Double similarity = ragDocumentHelper.resolveVectorScore(doc);
            if (similarity != null) {
                vectorSimilarities.add(similarity);
            }
        }
        return vectorSimilarities;
    }

    private double normalizeThreshold(Double configuredValue, double defaultValue) {
        if (configuredValue == null || configuredValue <= 0 || configuredValue > 1) {
            return defaultValue;
        }
        return configuredValue;
    }
}
