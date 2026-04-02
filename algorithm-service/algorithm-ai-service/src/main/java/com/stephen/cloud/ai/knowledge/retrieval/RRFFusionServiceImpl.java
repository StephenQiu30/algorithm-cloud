package com.stephen.cloud.ai.knowledge.retrieval;

import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.*;


/**
 * RRF（Reciprocal Rank Fusion）融合服务
 * <p>
 * 将向量检索和关键词检索的结果按 RRF 公式进行加权融合：
 * score(doc) = sum( weight / (k + rank_i + 1) )，其中 rank_i 为文档在各路结果中的排名。
 * 融合后通过 Min-Max 归一化将分数映射到 [0, 1] 区间。
 * </p>
 *
 * @author StephenQiu30
 * @see <a href="https://learn.microsoft.com/en-us/azure/search/hybrid-search-ranking">RRF 原理</a>
 */
@Service
public class RRFFusionServiceImpl implements RRFFusionService {

    @Resource
    private RagDocumentHelper ragDocumentHelper;

    @Override
    public List<Document> fuse(List<Document> vectorDocs, List<Document> keywordDocs, int finalTopK, int rrfK) {
        return fuse(vectorDocs, keywordDocs, finalTopK, rrfK, 1.0D, 1.0D);
    }

    /**
     * 加权 RRF 融合：合并向量和关键词两路检索结果
     * <p>
     * 1. 按排名累加 RRF 分数（支持不同权重）<br/>
     * 2. 同一文档被两路同时命中时，sourceType 标记为 "hybrid"<br/>
     * 3. 按 fusionScore 降序截取 topK 后做 Min-Max 归一化
     * </p>
     */
    @Override
    public List<Document> fuse(List<Document> vectorDocs, List<Document> keywordDocs, int finalTopK, int rrfK,
                               double vectorWeight, double keywordWeight) {
        Map<String, Document> docMap = new LinkedHashMap<>();
        Map<String, Double> scoreMap = new LinkedHashMap<>();
        accumulate(vectorDocs, "vector", docMap, scoreMap, rrfK, vectorWeight);
        accumulate(keywordDocs, "keyword", docMap, scoreMap, rrfK, keywordWeight);
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(scoreMap.entrySet());
        sorted.sort(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()));
        List<Document> result = new ArrayList<>();
        int limit = finalTopK <= 0 ? 5 : finalTopK;
        for (Map.Entry<String, Double> entry : sorted) {
            if (result.size() >= limit) {
                break;
            }
            Document doc = docMap.get(entry.getKey());
            if (doc == null) {
                continue;
            }
            doc.getMetadata().put(SCORE, entry.getValue());
            doc.getMetadata().put(FUSION_SCORE, entry.getValue());
            result.add(doc);
        }
        normalizeFusionScores(result);
        return result;
    }

    /**
     * 按 RRF 公式累加分数：weight / (k + rank + 1)
     * 同一文档被多路命中时，合并 metadata 并将 sourceType 标记为 "hybrid"
     */
    private void accumulate(List<Document> docs, String sourceType, Map<String, Document> docMap,
                            Map<String, Double> scoreMap, int rrfK, double weight) {
        if (CollUtil.isEmpty(docs)) {
            return;
        }
        int k = rrfK <= 0 ? 60 : rrfK;
        double w = weight <= 0 ? 1.0D : weight;
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            String key = buildKey(doc, i);
            Document existing = docMap.get(key);
            if (existing == null) {
                doc.getMetadata().putIfAbsent(SOURCE_TYPE, sourceType);
                docMap.put(key, doc);
            } else {
                String existingSource = String.valueOf(existing.getMetadata().get(SOURCE_TYPE));
                String mergedSourceType = StringUtils.equals(existingSource, sourceType) ? existingSource : "hybrid";
                ragDocumentHelper.mergeMetadata(existing, doc, mergedSourceType);
            }
            double delta = w / (k + i + 1);
            scoreMap.put(key, scoreMap.getOrDefault(key, 0D) + delta);
        }
    }

    /**
     * 构建文档去重键：优先使用 chunkId，兜底使用 content hash
     */
    private String buildKey(Document doc, int index) {
        String key = ragDocumentHelper.buildDocKey(doc);
        if (StringUtils.isNotBlank(key)) {
            return key;
        }
        Object esId = doc.getMetadata().get(ES_ID);
        if (esId != null && StringUtils.isNotBlank(String.valueOf(esId))) {
            return String.valueOf(esId);
        }
        if (StringUtils.isNotBlank(doc.getId())) {
            return doc.getId();
        }
        return "fallback_" + index + "_" + (doc.getText() != null ? doc.getText().hashCode() : "null");
    }

    /**
     * Min-Max 归一化：将 fusionScore 映射到 [0, 1] 区间
     */
    private void normalizeFusionScores(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return;
        }
        // 单结果场景：固定为满分 1.0，保证与多结果归一化行为一致
        if (docs.size() == 1) {
            docs.getFirst().getMetadata().put(FUSION_SCORE, 1.0D);
            docs.getFirst().getMetadata().put(SCORE, 1.0D);
            return;
        }
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (Document doc : docs) {
            Object score = doc.getMetadata().get(FUSION_SCORE);
            if (score != null) {
                double val = Double.parseDouble(String.valueOf(score));
                min = Math.min(min, val);
                max = Math.max(max, val);
            }
        }
        if (max <= min) {
            return;
        }
        for (Document doc : docs) {
            Object score = doc.getMetadata().get(FUSION_SCORE);
            if (score != null) {
                double val = Double.parseDouble(String.valueOf(score));
                double normalized = (val - min) / (max - min);
                doc.getMetadata().put(FUSION_SCORE, normalized);
                doc.getMetadata().put(SCORE, normalized);
            }
        }
    }
}
