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
     * 是否开启 LLM 语义改写（默认关闭，确保向下兼容）
     */
    private boolean llmRewriteEnabled = false;
}
