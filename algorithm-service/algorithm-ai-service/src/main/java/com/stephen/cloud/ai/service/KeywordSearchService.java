package com.stephen.cloud.ai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;

/**
 * 关键词检索服务接口
 * <p>
 * 提供基于 BM25 算法的关键词检索能力
 * </p>
 *
 * @author StephenQiu30
 */
public interface KeywordSearchService {

    /**
     * BM25 关键词检索
     *
     * @param query            查询文本
     * @param knowledgeBaseId  知识库 ID
     * @param topK             返回数量
     * @param filterExpression 过滤表达式
     * @return 匹配的文档片段列表
     */
    List<Document> bm25Search(String query, Long knowledgeBaseId, Integer topK, Filter.Expression filterExpression);
}
