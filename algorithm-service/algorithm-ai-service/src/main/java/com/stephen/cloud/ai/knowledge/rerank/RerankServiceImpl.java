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
        for (Document doc : docs) {
            doc.getMetadata().put("rerankScore", calculateScore(doc, mustTerms, metadataFilters));
        }
        docs.sort(Comparator.comparingDouble(
                (Document doc) -> parseDouble(doc.getMetadata().get("rerankScore"))).reversed());
        int limit = finalTopK <= 0 ? 5 : finalTopK;
        if (docs.size() > limit) {
            return docs.subList(0, limit);
        }
        return docs;
    }

    private double calculateScore(Document doc, List<String> mustTerms, Map<String, String> metadataFilters) {
        double baseScore = parseDouble(doc.getMetadata().get("fusionScore"));
        double mustTermScore = computeMustTermScore(doc, mustTerms);
        double metadataScore = computeMetadataScore(doc, metadataFilters);
        double titleScore = computeTitleScore(doc, mustTerms);
        double mustTermBoost = ragRetrievalProperties.getMustTermBoost() == null ? 0.2D
                : ragRetrievalProperties.getMustTermBoost();
        double metadataMatchBoost = ragRetrievalProperties.getMetadataMatchBoost() == null ? 0.1D
                : ragRetrievalProperties.getMetadataMatchBoost();
        return baseScore
                + mustTermScore * mustTermBoost
                + metadataScore * metadataMatchBoost
                + titleScore * 0.05D;
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
        List<String> candidates = new ArrayList<>(3);
        Object[] candidateValues = {
                doc.getMetadata().get("documentName"),
                doc.getMetadata().get("sectionTitle"),
                doc.getMetadata().get("sectionPath")
        };
        for (Object candidateValue : candidateValues) {
            if (candidateValue == null) {
                continue;
            }
            String candidate = String.valueOf(candidateValue).trim();
            if (StringUtils.isNotBlank(candidate)) {
                candidates.add(candidate.toLowerCase());
            }
        }
        if (candidates.isEmpty()) {
            return 0D;
        }
        long matched = 0;
        for (String mustTerm : mustTerms) {
            if (StringUtils.isBlank(mustTerm)) {
                continue;
            }
            String normalizedMustTerm = mustTerm.toLowerCase();
            for (String candidate : candidates) {
                if (candidate.contains(normalizedMustTerm)) {
                    matched++;
                    break;
                }
            }
        }
        return matched == 0 ? 0D : matched * 1.0D / mustTerms.size();
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
}
