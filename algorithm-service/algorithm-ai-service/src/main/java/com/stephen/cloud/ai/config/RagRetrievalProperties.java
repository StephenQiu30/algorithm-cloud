package com.stephen.cloud.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag.retrieval")
public class RagRetrievalProperties {

    private int topK = 5;

    private int vectorTopK = 10;

    private int keywordTopK = 10;

    private int rrfK = 60;

    private Double similarityThreshold = 0.7D;

    private boolean rewriteEnabled = true;

    private boolean rerankEnabled = true;

    private int rerankTopN = 10;

    private Double mustTermBoost = 0.2D;

    private Double metadataMatchBoost = 0.1D;

    private String indexName = "document_chunks";

    /**
     * 向量检索在 RRF 融合中的权重
     */
    private double vectorWeight = 1.0D;

    /**
     * 关键词检索在 RRF 融合中的权重
     */
    private double keywordWeight = 1.0D;

    /**
     * 是否开启 Multi-Query 扩展召回
     */
    private boolean multiQueryEnabled = true;

    /**
     * Multi-Query 时是否保留原始问题作为召回候选，降低改写偏移导致的漏召回。
     */
    private boolean includeOriginalQuery = true;

    /**
     * 是否开启 LLM 语义改写（默认关闭，确保向下兼容）
     */
    private boolean llmRewriteEnabled = false;

    /**
     * 低召回时是否自动放宽向量阈值进行二次补召回。
     */
    private boolean recallFallbackEnabled = true;

    /**
     * 触发补召回的最小命中数阈值。
     */
    private int recallFallbackMinHits = 3;

    /**
     * 二次补召回使用的相似度阈值下限。
     */
    private Double fallbackSimilarityThreshold = 0.55D;

    /**
     * 复杂问题最少召回片段数。
     */
    private int complexQueryTopK = 12;
}
