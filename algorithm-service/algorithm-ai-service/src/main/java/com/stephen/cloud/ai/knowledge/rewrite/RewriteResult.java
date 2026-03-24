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
}
