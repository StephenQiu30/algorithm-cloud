package com.stephen.cloud.ai.knowledge.retrieval;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/**
 * RAG 文档助手：统一处理稳定分片 ID、得分解析与元数据合并。
 */
@Component
public class RagDocumentHelper {

    public String resolveChunkId(Document doc) {
        if (doc == null) {
            return null;
        }
        Object chunkId = doc.getMetadata().get("chunkId");
        if (chunkId != null && StringUtils.isNotBlank(String.valueOf(chunkId))) {
            return String.valueOf(chunkId);
        }
        Object vectorId = doc.getMetadata().get("vectorId");
        if (vectorId != null && StringUtils.isNotBlank(String.valueOf(vectorId))) {
            return String.valueOf(vectorId);
        }
        Object documentId = doc.getMetadata().get("documentId");
        Object chunkIndex = doc.getMetadata().get("chunkIndex");
        if (documentId != null && chunkIndex != null) {
            return documentId + "_" + chunkIndex;
        }
        if (StringUtils.isNotBlank(doc.getId())) {
            return doc.getId();
        }
        return doc.getText() == null ? null : "text_" + doc.getText().hashCode();
    }

    public String buildDocKey(Document doc) {
        String chunkId = resolveChunkId(doc);
        if (StringUtils.isNotBlank(chunkId)) {
            return chunkId;
        }
        return "fallback_" + System.identityHashCode(doc);
    }

    public Double resolveVectorScore(Document doc) {
        if (doc == null) {
            return null;
        }
        if (doc.getScore() != null) {
            return doc.getScore();
        }
        Double vectorScore = toDouble(doc.getMetadata().get("vectorScore"));
        if (vectorScore != null) {
            return vectorScore;
        }
        Double distance = resolveVectorDistance(doc);
        if (distance != null) {
            return 1.0D - distance;
        }
        return null;
    }

    public Double resolveVectorDistance(Document doc) {
        if (doc == null) {
            return null;
        }
        return toDouble(doc.getMetadata().get("distance"));
    }

    public Double resolveKeywordScore(Document doc) {
        if (doc == null) {
            return null;
        }
        return toDouble(doc.getMetadata().get("keywordScore"));
    }

    public Double resolveFusionScore(Document doc) {
        if (doc == null) {
            return null;
        }
        Object fusionScore = doc.getMetadata().get("fusionScore");
        if (fusionScore != null) {
            return toDouble(fusionScore);
        }
        return toDouble(doc.getMetadata().get("score"));
    }

    public void mergeMetadata(Document target, Document source, String mergedSourceType) {
        if (target == null || source == null) {
            return;
        }
        source.getMetadata().forEach((key, value) -> {
            if (value != null) {
                target.getMetadata().putIfAbsent(key, value);
            }
        });
        if (StringUtils.isNotBlank(mergedSourceType)) {
            target.getMetadata().put("sourceType", mergedSourceType);
        }
        Double sourceVectorScore = resolveVectorScore(source);
        if (resolveVectorScore(target) == null && sourceVectorScore != null) {
            target.getMetadata().put("vectorScore", sourceVectorScore);
            Double sourceDistance = resolveVectorDistance(source);
            if (sourceDistance != null) {
                target.getMetadata().put("distance", sourceDistance);
            } else {
                target.getMetadata().put("distance", 1.0D - sourceVectorScore);
            }
        }
        if (resolveKeywordScore(target) == null && resolveKeywordScore(source) != null) {
            target.getMetadata().put("keywordScore", resolveKeywordScore(source));
        }
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
