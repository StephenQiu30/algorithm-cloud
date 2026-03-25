package com.stephen.cloud.ai.knowledge.rewrite;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RewriteResult {

    private String semanticQuery;

    private String keywordQuery;

    private List<String> mustTerms;

    private Map<String, String> metadataFilters;

    /**
     * LLM 拆分的子查询列表（Multi-Query 扩展使用）
     */
    private List<String> subQueries;
}
