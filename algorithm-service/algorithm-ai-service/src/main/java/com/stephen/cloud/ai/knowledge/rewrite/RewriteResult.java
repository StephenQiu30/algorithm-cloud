package com.stephen.cloud.ai.knowledge.rewrite;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 查询改写结果
 * <p>
 * 包含语义查询、关键词查询、必要术语、元数据过滤条件等
 * </p>
 *
 * @author StephenQiu30
 */
@Data
public class RewriteResult {

    /**
     * 语义查询（用于向量检索）
     */
    private String semanticQuery;

    /**
     * 关键词查询（用于 BM25 检索）
     */
    private String keywordQuery;

    /**
     * 必要术语列表
     */
    private List<String> mustTerms;

    /**
     * 元数据过滤条件
     */
    private Map<String, String> metadataFilters;

    /**
     * LLM 拆分的子查询列表（Multi-Query 扩展使用）
     */
    private List<String> subQueries;
}
