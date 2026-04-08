package com.stephen.cloud.ai.knowledge.retrieval;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * RRF（Reciprocal Rank Fusion）融合服务
 * <p>
 * 将向量检索和关键词检索的结果进行融合排序，提升召回质量
 * </p>
 *
 * @author StephenQiu30
 */
public interface RRFFusionService {

    /**
     * 标准 RRF 融合
     *
     * @param vectorDocs  向量检索结果
     * @param keywordDocs 关键词检索结果
     * @param finalTopK   最终返回数量
     * @param rrfK        RRF 平滑参数
     * @return 融合后的文档列表
     */
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
