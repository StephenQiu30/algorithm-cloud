package com.stephen.cloud.ai.knowledge.retrieval;

import cn.hutool.core.collection.CollUtil;
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
        Map<String, Document> docMap = new LinkedHashMap<>();
        Map<String, Double> scoreMap = new LinkedHashMap<>();
        accumulate(vectorDocs, "vector", docMap, scoreMap, rrfK);
        accumulate(keywordDocs, "keyword", docMap, scoreMap, rrfK);
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
        return result;
    }

    private void accumulate(List<Document> docs, String sourceType, Map<String, Document> docMap,
                            Map<String, Double> scoreMap, int rrfK) {
        if (CollUtil.isEmpty(docs)) {
            return;
        }
        int k = rrfK <= 0 ? 60 : rrfK;
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            String key = buildKey(doc, i);
            doc.getMetadata().putIfAbsent("sourceType", sourceType);
            docMap.putIfAbsent(key, doc);
            double delta = 1.0D / (k + i + 1);
            scoreMap.put(key, scoreMap.getOrDefault(key, 0D) + delta);
        }
    }

    private String buildKey(Document doc, int index) {
        Object chunkId = doc.getMetadata().get("chunkId");
        if (chunkId != null) {
            return String.valueOf(chunkId);
        }
        Object documentId = doc.getMetadata().get("documentId");
        Object chunkIndex = doc.getMetadata().get("chunkIndex");
        if (documentId != null || chunkIndex != null) {
            return String.valueOf(documentId) + "_" + String.valueOf(chunkIndex);
        }
        Object esId = doc.getMetadata().get("esId");
        if (esId != null) {
            return String.valueOf(esId);
        }
        return "fallback_" + index + "_" + doc.getText().hashCode();
    }
}
