package com.stephen.cloud.ai.knowledge.retrieval;

import cn.hutool.core.collection.CollUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RRFFusionServiceImpl implements RRFFusionService {

    @Override
    public List<Document> fuse(List<Document> vectorDocs, List<Document> keywordDocs, int finalTopK, int rrfK) {
        return fuse(vectorDocs, keywordDocs, finalTopK, rrfK, 1.0D, 1.0D);
    }

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
            doc.getMetadata().put("score", entry.getValue());
            doc.getMetadata().put("fusionScore", entry.getValue());
            result.add(doc);
        }
        // Min-Max 归一化: fusionScore → [0, 1]
        normalizeFusionScores(result);
        return result;
    }

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
            doc.getMetadata().putIfAbsent("sourceType", sourceType);
            docMap.putIfAbsent(key, doc);
            double delta = w / (k + i + 1);
            scoreMap.put(key, scoreMap.getOrDefault(key, 0D) + delta);
        }
    }

    private String buildKey(Document doc, int index) {
        // 优先使用 chunkId (ETL 过程中生成)
        Object chunkId = doc.getMetadata().get("chunkId");
        if (chunkId != null && StringUtils.isNotBlank(String.valueOf(chunkId))) {
            return String.valueOf(chunkId);
        }
        
        // 其次使用 documentId + chunkIndex
        Object documentId = doc.getMetadata().get("documentId");
        Object chunkIndex = doc.getMetadata().get("chunkIndex");
        if (documentId != null && chunkIndex != null) {
            return documentId + "_" + chunkIndex;
        }
        
        // 再次考虑 esId (从 ES 检索结果中带出)
        Object esId = doc.getMetadata().get("esId");
        if (esId != null && StringUtils.isNotBlank(String.valueOf(esId))) {
            return String.valueOf(esId);
        }
        
        // 最后兜底：ID 或 内容哈希
        if (StringUtils.isNotBlank(doc.getId())) {
            return doc.getId();
        }
        return "fallback_" + index + "_" + (doc.getText() != null ? doc.getText().hashCode() : "null");
    }

    /**
     * Min-Max 归一化：将 fusionScore 映射到 [0, 1] 区间
     */
    private void normalizeFusionScores(List<Document> docs) {
        if (docs == null || docs.size() <= 1) {
            return;
        }
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (Document doc : docs) {
            Object score = doc.getMetadata().get("fusionScore");
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
            Object score = doc.getMetadata().get("fusionScore");
            if (score != null) {
                double val = Double.parseDouble(String.valueOf(score));
                double normalized = (val - min) / (max - min);
                doc.getMetadata().put("fusionScore", normalized);
                doc.getMetadata().put("score", normalized);
            }
        }
    }
}
