package com.stephen.cloud.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 检索配置属性
 * <p>
 * 通过 Nacos 动态注入（prefix = rag.retrieval），支持多环境覆盖。
 * 所有默认值为 MVP 基线，生产环境通过 common-ai-prod.yml 覆盖差异项。
 * </p>
 *
 * @author StephenQiu30
 */
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

    private String vectorIndexName = "document_chunks_vector";

    private String keywordIndexName = "document_chunks_search";

    /**
     * 向量检索在 RRF 融合中的权重
     */
    private double vectorWeight = 1.0D;

    /**
     * 关键词检索在 RRF 融合中的权重
     */
    private double keywordWeight = 1.0D;

    /**
     * 是否开启 Multi-Query 扩展召回（默认关闭，需通过 Nacos 显式开启）
     */
    private boolean multiQueryEnabled = false;

    /**
     * Multi-Query 时是否保留原始问题作为召回候选（默认关闭，跟随 multiQueryEnabled）
     */
    private boolean includeOriginalQuery = false;

    /**
     * 是否开启 LLM 语义改写（默认关闭，确保向下兼容）
     */
    private boolean llmRewriteEnabled = false;

    /**
     * 低召回时是否自动放宽向量阈值进行二次补召回（默认关闭，需通过 Nacos 显式开启）
     */
    private boolean recallFallbackEnabled = false;

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

    /**
     * 检索超时时间（秒），超时后降级为空结果
     */
    private int retrievalTimeoutSeconds = 3;

    /**
     * 复杂查询判断的最小长度阈值
     */
    private int complexQueryMinLength = 18;

    /**
     * 复杂查询标记关键词列表（通过 Nacos 动态配置）
     */
    private List<String> complexQueryMarkers = List.of(
            "列举", "总结", "汇总", "梳理", "比较", "对比", "区别", "差异",
            "优点", "优势", "缺点", "原因", "为什么", "如何", "步骤", "流程",
            "有哪些", "哪些", "全部", "完整", "详细", "全面"
    );

    /**
     * 同义词映射表（通过 Nacos 动态配置，key=原词, value=扩展同义词列表）
     */
    private Map<String, List<String>> synonymMap = new LinkedHashMap<>(Map.of(
            "异常", List.of("错误", "故障"),
            "报错", List.of("错误", "异常"),
            "失败", List.of("错误", "故障"),
            "优化", List.of("提升", "性能"),
            "配置", List.of("参数", "设置")
    ));
}
