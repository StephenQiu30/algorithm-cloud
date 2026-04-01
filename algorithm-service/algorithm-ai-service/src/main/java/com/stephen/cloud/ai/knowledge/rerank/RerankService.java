package com.stephen.cloud.ai.knowledge.rerank;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

public interface RerankService {

    /**
     * 重新排序融合文档
     *
     * @param fusedDocs       RRF 融合后的候选文档
     * @param originalQuery   用户原始查询（用于 query-document 相关性评估）
     * @param mustTerms       必要术语列表
     * @param metadataFilters 元数据过滤条件
     * @param finalTopK       最终返回数量
     * @return 重排后的文档列表
     */
    List<Document> rerank(List<Document> fusedDocs, String originalQuery, List<String> mustTerms,
                          Map<String, String> metadataFilters, int finalTopK);
}
