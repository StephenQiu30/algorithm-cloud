package com.stephen.cloud.ai.knowledge.retrieval;

import org.springframework.ai.document.Document;

import java.util.List;

public interface RRFFusionService {

    List<Document> fuse(List<Document> vectorDocs, List<Document> keywordDocs, int finalTopK, int rrfK);

    /**
     * 加权 RRF 融合
     *
     * @param vectorWeight  向量检索权重
     * @param keywordWeight 关键词检索权重
     */
    List<Document> fuse(List<Document> vectorDocs, List<Document> keywordDocs, int finalTopK, int rrfK,
                        double vectorWeight, double keywordWeight);
}
