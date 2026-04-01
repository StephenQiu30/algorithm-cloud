package com.stephen.cloud.ai.knowledge.retrieval;

import com.stephen.cloud.api.ai.model.vo.ChunkVO;
import com.stephen.cloud.api.ai.model.vo.RetrievalHitVO;
import com.stephen.cloud.api.ai.model.vo.SourceVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.*;

/**
 * RAG 文档助手
 * <p>
 * 统一处理：稳定分片 ID 解析、得分解析、元数据合并、Document → VO 转换。
 * 所有与 Document metadata 交互的逻辑应集中在此类，避免各 Service 中重复硬编码字段名。
 * </p>
 *
 * @author StephenQiu30
 */
@Component
public class RagDocumentHelper {

    // ==================== ID 解析 ====================

    public String resolveChunkId(Document doc) {
        if (doc == null) {
            return null;
        }
        String chunkId = toStr(doc.getMetadata().get(CHUNK_ID));
        if (StringUtils.isNotBlank(chunkId)) {
            return chunkId;
        }
        String vectorId = toStr(doc.getMetadata().get(VECTOR_ID));
        if (StringUtils.isNotBlank(vectorId)) {
            return vectorId;
        }
        Object documentId = doc.getMetadata().get(DOCUMENT_ID);
        Object chunkIndex = doc.getMetadata().get(CHUNK_INDEX);
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

    // ==================== 得分解析 ====================

    public Double resolveVectorScore(Document doc) {
        if (doc == null) {
            return null;
        }
        if (doc.getScore() != null) {
            return doc.getScore();
        }
        Double vectorScore = toDouble(doc.getMetadata().get(VECTOR_SCORE));
        if (vectorScore != null) {
            return vectorScore;
        }
        Double distance = toDouble(doc.getMetadata().get(DISTANCE));
        if (distance != null) {
            return 1.0D - distance;
        }
        return null;
    }

    public Double resolveKeywordScore(Document doc) {
        return doc == null ? null : toDouble(doc.getMetadata().get(KEYWORD_SCORE));
    }

    public Double resolveFusionScore(Document doc) {
        if (doc == null) {
            return null;
        }
        Double fusionScore = toDouble(doc.getMetadata().get(FUSION_SCORE));
        return fusionScore != null ? fusionScore : toDouble(doc.getMetadata().get(SCORE));
    }

    public Double resolveDefaultScore(Document doc) {
        Double fusionScore = resolveFusionScore(doc);
        if (fusionScore != null) {
            return fusionScore;
        }
        Double vectorScore = resolveVectorScore(doc);
        return vectorScore != null ? vectorScore : resolveKeywordScore(doc);
    }

    // ==================== 元数据合并 ====================

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
            target.getMetadata().put(SOURCE_TYPE, mergedSourceType);
        }
        Double sourceVectorScore = resolveVectorScore(source);
        if (resolveVectorScore(target) == null && sourceVectorScore != null) {
            target.getMetadata().put(VECTOR_SCORE, sourceVectorScore);
            Double sourceDistance = toDouble(source.getMetadata().get(DISTANCE));
            target.getMetadata().put(DISTANCE,
                    sourceDistance != null ? sourceDistance : 1.0D - sourceVectorScore);
        }
        if (resolveKeywordScore(target) == null && resolveKeywordScore(source) != null) {
            target.getMetadata().put(KEYWORD_SCORE, resolveKeywordScore(source));
        }
    }

    // ==================== Document → VO 转换（统一入口）====================

    /**
     * Document → RetrievalHitVO（用于召回分析各阶段展示）
     */
    public RetrievalHitVO toRetrievalHitVO(Document doc) {
        if (doc == null) {
            return null;
        }
        Map<String, Object> meta = doc.getMetadata();
        RetrievalHitVO hit = new RetrievalHitVO();
        String chunkId = resolveChunkId(doc);
        hit.setId(chunkId);
        hit.setChunkId(chunkId);
        hit.setDocumentId(toLong(meta.get(DOCUMENT_ID)));
        hit.setDocumentName(toStr(meta.get(DOCUMENT_NAME)));
        hit.setChunkIndex(toInteger(meta.get(CHUNK_INDEX)));
        hit.setSectionTitle(toStr(meta.get(SECTION_TITLE)));
        hit.setSectionPath(toStr(meta.get(SECTION_PATH)));
        hit.setContent(doc.getText());
        hit.setVectorScore(resolveVectorScore(doc));
        hit.setKeywordScore(resolveKeywordScore(doc));
        hit.setFusionScore(resolveFusionScore(doc));
        hit.setScore(resolveDefaultScore(doc));
        hit.setSimilarityScore(resolveVectorScore(doc));
        hit.setRerankScore(toDouble(meta.get(RERANK_SCORE)));
        hit.setMatchReason(toStr(meta.get(MATCH_REASON)));
        return hit;
    }

    /**
     * Document 列表 → RetrievalHitVO 列表
     */
    public List<RetrievalHitVO> toRetrievalHitVOs(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return new ArrayList<>();
        }
        return docs.stream().map(this::toRetrievalHitVO).toList();
    }

    /**
     * Document → SourceVO（用于流式问答的引用来源展示）
     */
    public SourceVO toSourceVO(Document doc) {
        if (doc == null) {
            return null;
        }
        Map<String, Object> meta = doc.getMetadata();
        SourceVO source = new SourceVO();
        source.setDocumentId(toLong(meta.get(DOCUMENT_ID)));
        source.setDocumentName(toStr(meta.get(DOCUMENT_NAME)));
        source.setChunkIndex(toInteger(meta.get(CHUNK_INDEX)));
        source.setChunkId(resolveChunkId(doc));
        source.setContent(doc.getText());
        source.setScore(resolveDefaultScore(doc));
        source.setVectorSimilarity(resolveVectorScore(doc));
        source.setKeywordRelevance(resolveKeywordScore(doc));
        source.setSourceType(toStr(meta.get(SOURCE_TYPE)));
        source.setVersion(toStr(meta.get(VERSION)));
        source.setBizTag(toStr(meta.get(BIZ_TAG)));
        source.setSectionTitle(toStr(meta.get(SECTION_TITLE)));
        source.setSectionPath(toStr(meta.get(SECTION_PATH)));
        source.setMatchReason(toStr(meta.get(MATCH_REASON)));
        return source;
    }

    /**
     * Document 列表 → SourceVO 列表
     */
    public List<SourceVO> toSourceVOs(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return new ArrayList<>();
        }
        return docs.stream().map(this::toSourceVO).toList();
    }

    /**
     * Document → ChunkVO（用于分片搜索展示）
     */
    public ChunkVO toChunkVO(Document doc) {
        if (doc == null) {
            return null;
        }
        Map<String, Object> meta = doc.getMetadata();
        ChunkVO vo = new ChunkVO();
        String chunkId = resolveChunkId(doc);
        vo.setId(StringUtils.defaultIfBlank(doc.getId(), chunkId));
        vo.setChunkId(chunkId);
        vo.setDocumentId(toLong(meta.get(DOCUMENT_ID)));
        vo.setDocumentName(toStr(meta.get(DOCUMENT_NAME)));
        vo.setChunkIndex(toInteger(meta.get(CHUNK_INDEX)));
        vo.setKnowledgeBaseId(toLong(meta.get(KNOWLEDGE_BASE_ID)));
        vo.setSectionTitle(toStr(meta.get(SECTION_TITLE)));
        vo.setSectionPath(toStr(meta.get(SECTION_PATH)));
        vo.setContent(doc.getText());
        vo.setWordCount(doc.getText() == null ? 0 : doc.getText().length());
        vo.setScore(resolveFusionScore(doc));
        vo.setSourceType(toStr(meta.get(SOURCE_TYPE)));
        vo.setMatchReason(toStr(meta.get(MATCH_REASON)));
        return vo;
    }

    /**
     * Document 列表 → ChunkVO 列表
     */
    public List<ChunkVO> toChunkVOs(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return new ArrayList<>();
        }
        return docs.stream().map(this::toChunkVO).toList();
    }

    // ==================== 类型安全工具方法 ====================

    private String toStr(Object value) {
        return value == null ? null : String.valueOf(value);
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

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
