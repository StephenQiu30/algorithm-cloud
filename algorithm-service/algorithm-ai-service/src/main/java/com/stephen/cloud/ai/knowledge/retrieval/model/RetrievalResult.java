package com.stephen.cloud.ai.knowledge.retrieval.model;

import lombok.Data;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 检索结果封装
 * <p>
 * 包含最终文档列表和各阶段中间数据，供召回分析使用。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
public class RetrievalResult {

    /**
     * 最终文档列表
     */
    private List<Document> docs;

    /**
     * 向量检索命中文档（中间数据）
     */
    private List<Document> vectorDocs;

    /**
     * 关键词检索命中文档（中间数据）
     */
    private List<Document> keywordDocs;

    /**
     * RRF 融合后文档（中间数据）
     */
    private List<Document> fusedDocs;

    /**
     * 改写后的关键词查询
     */
    private String rewriteQuery;

    /**
     * 改写后的语义查询
     */
    private String rewriteSemanticQuery;

    /**
     * 检索元数据 JSON
     */
    private String retrievalMeta;

    /**
     * 使用的检索策略
     */
    private String retrievalStrategy;
}
