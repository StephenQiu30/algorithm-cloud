package com.stephen.cloud.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 联网搜索兜底配置
 *
 * @author StephenQiu30
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag.web-search")
public class RagWebSearchProperties {

    /**
     * 是否启用联网搜索兜底。
     */
    private boolean enabled = true;

    /**
     * 当知识库完全无召回时是否触发联网搜索。
     */
    private boolean fallbackOnEmpty = true;

    /**
     * 当召回数量少且向量相关性不足时是否触发联网搜索。
     */
    private boolean fallbackOnLowConfidence = true;

    /**
     * 认为“知识库已有足够上下文”的最小命中数。
     */
    private int minKnowledgeHits = 2;

    /**
     * 触发低置信兜底的最高向量相似度阈值。
     */
    private Double minTopSimilarity = 0.72D;

    /**
     * 触发低置信兜底的平均向量相似度阈值。
     */
    private Double minAverageSimilarity = 0.65D;
}
