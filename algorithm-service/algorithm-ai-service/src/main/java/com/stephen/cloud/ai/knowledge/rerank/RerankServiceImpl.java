package com.stephen.cloud.ai.knowledge.rerank;

import cn.hutool.core.collection.CollUtil;
import com.stephen.cloud.ai.config.RagRetrievalProperties;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class RerankServiceImpl implements RerankService {

    @Resource
    private RagRetrievalProperties ragRetrievalProperties;

    @Override
    public List<Document> rerank(List<Document> fusedDocs, List<String> mustTerms, Map<String, String> metadataFilters, int finalTopK) {
        if (CollUtil.isEmpty(fusedDocs)) {
            return List.of();
        }
        List<Document> docs = new ArrayList<>(fusedDocs);
        docs.sort(Comparator.comparingDouble(doc -> -score(doc, mustTerms, metadataFilters)));
        int limit = finalTopK <= 0 ? 5 : finalTopK;
        if (docs.size() > limit) {
            return docs.subList(0, limit);
        }
        return docs;
    }

    private double score(Document doc, List<String> mustTerms, Map<String, String> metadataFilters) {
        double baseScore = parseDouble(doc.getMetadata().get("fusionScore"));
        double mustTermScore = computeMustTermScore(doc, mustTerms);
        double metadataScore = computeMetadataScore(doc, metadataFilters);
        double titleScore = computeTitleScore(doc, mustTerms);
        double score = baseScore
                + mustTermScore * defaultDouble(ragRetrievalProperties.getMustTermBoost(), 0.2D)
                + metadataScore * defaultDouble(ragRetrievalProperties.getMetadataMatchBoost(), 0.1D)
                + titleScore * 0.05D;
        doc.getMetadata().put("rerankScore", score);
        return score;
    }

    private double computeMustTermScore(Document doc, List<String> mustTerms) {
        if (CollUtil.isEmpty(mustTerms)) {
            return 0D;
        }
        String text = StringUtils.defaultString(doc.getText()).toLowerCase();
        long matched = mustTerms.stream().filter(StringUtils::isNotBlank)
                .map(String::toLowerCase)
                .filter(text::contains)
                .count();
        return matched * 1.0D / mustTerms.size();
    }

    private double computeMetadataScore(Document doc, Map<String, String> metadataFilters) {
        if (metadataFilters == null || metadataFilters.isEmpty()) {
            return 0D;
        }
        long matched = metadataFilters.entrySet().stream()
                .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
                .filter(entry -> StringUtils.containsIgnoreCase(
                        String.valueOf(doc.getMetadata().get(entry.getKey())),
                        entry.getValue()))
                .count();
        return matched * 1.0D / metadataFilters.size();
    }

    private double computeTitleScore(Document doc, List<String> mustTerms) {
        if (CollUtil.isEmpty(mustTerms)) {
            return 0D;
        }
        String title = String.valueOf(doc.getMetadata().get("documentName"));
        if (StringUtils.isBlank(title)) {
            return 0D;
        }
        String lowerTitle = title.toLowerCase();
        return mustTerms.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::toLowerCase)
                .anyMatch(lowerTitle::contains) ? 1D : 0D;
    }

    private double parseDouble(Object value) {
        if (value == null) {
            return 0D;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0D;
        }
    }

    private double defaultDouble(Double value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
