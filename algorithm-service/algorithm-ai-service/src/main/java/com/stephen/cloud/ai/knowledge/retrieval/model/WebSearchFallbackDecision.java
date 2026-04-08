package com.stephen.cloud.ai.knowledge.retrieval.model;

/**
 * 联网搜索兜底判定结果
 *
 * @author StephenQiu30
 */
public record WebSearchFallbackDecision(
        boolean shouldFallback,
        String reason,
        int knowledgeHitCount,
        Double topVectorSimilarity,
        Double averageVectorSimilarity
) {

    public static final String REQUEST_DISABLED = "REQUEST_DISABLED";
    public static final String GLOBAL_DISABLED = "GLOBAL_DISABLED";
    public static final String EMPTY_RECALL = "EMPTY_RECALL";
    public static final String LOW_CONFIDENCE_RECALL = "LOW_CONFIDENCE_RECALL";
    public static final String NO_VECTOR_SIGNAL = "NO_VECTOR_SIGNAL";
    public static final String ENOUGH_CONTEXT = "ENOUGH_CONTEXT";
}
