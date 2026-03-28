package com.stephen.cloud.ai.knowledge.retrieval;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.stephen.cloud.ai.config.RagRetrievalProperties;
import com.stephen.cloud.ai.knowledge.rerank.RerankService;
import com.stephen.cloud.ai.knowledge.retrieval.model.RetrievalResult;
import com.stephen.cloud.ai.knowledge.rewrite.QueryRewriteService;
import com.stephen.cloud.ai.knowledge.rewrite.RewriteResult;
import com.stephen.cloud.ai.service.KeywordSearchService;
import com.stephen.cloud.ai.service.VectorStoreService;
import com.stephen.cloud.api.ai.model.enums.MatchReasonEnum;
import com.stephen.cloud.api.ai.model.enums.RetrievalStrategyEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 检索编排器
 * <p>
 * 统一编排 RAG 检索的完整链路：
 * Query改写 → Multi-Query向量检索 → 关键词BM25检索 → 加权RRF融合 → Rerank → 标记命中原因
 * </p>
 * <p>
 * 消除原来 RAGServiceImpl 中流式问答与召回分析两处重复的检索逻辑。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class RetrievalOrchestrator {

    private static final List<String> COMPLEX_QUERY_MARKERS = List.of(
            "列举", "总结", "汇总", "梳理", "比较", "对比", "区别", "差异",
            "优点", "优势", "缺点", "原因", "为什么", "如何", "步骤", "流程",
            "有哪些", "哪些", "全部", "完整", "详细", "全面"
    );

    @Resource
    private VectorStoreService vectorStoreService;

    @Resource
    private KeywordSearchService keywordSearchService;

    @Resource
    private RRFFusionService rrfFusionService;

    @Resource
    private RagRetrievalProperties ragRetrievalProperties;

    @Resource
    private QueryRewriteService queryRewriteService;

    @Resource
    private RerankService rerankService;

    @Resource
    private RagDocumentHelper ragDocumentHelper;

    /**
     * 执行完整的检索编排
     *
     * @param question        用户原始问题
     * @param knowledgeBaseId 知识库ID
     * @param topK            最终返回数量（null 使用默认）
     * @param similarityThreshold 相似度阈值（null 使用默认）
     * @param enableRerank    是否启用重排（null 使用配置）
     * @param history         会话历史（用于多轮问题压缩检索）
     * @return 检索结果（包含各阶段中间数据）
     */
    public RetrievalResult retrieve(String question, Long knowledgeBaseId, Integer topK,
                                     Double similarityThreshold, Boolean enableRerank, List<Message> history) {
        boolean complexQuery = isComplexQuery(question);
        int finalTopK = resolveTopK(topK, complexQuery);
        int vectorTopK = Math.max(ragRetrievalProperties.getVectorTopK() <= 0 ? finalTopK
                : ragRetrievalProperties.getVectorTopK(), finalTopK);
        int keywordTopK = Math.max(ragRetrievalProperties.getKeywordTopK() <= 0 ? finalTopK
                : ragRetrievalProperties.getKeywordTopK(), finalTopK);
        int rrfK = ragRetrievalProperties.getRrfK() <= 0 ? 60 : ragRetrievalProperties.getRrfK();
        Double threshold = similarityThreshold != null ? similarityThreshold : ragRetrievalProperties.getSimilarityThreshold();

        // 1. Query 改写
        RewriteResult rewriteResult = buildRewriteResult(question, history);
        Filter.Expression filter = buildFilterExpression(knowledgeBaseId, rewriteResult.getMetadataFilters());

        // 2. 向量检索（支持 Multi-Query 扩展）
        List<Document> vectorDocs = executeVectorSearch(question, rewriteResult, filter, vectorTopK, threshold);

        // 3. 关键词 BM25 检索
        List<Document> keywordDocs = keywordSearchService.bm25Search(
                rewriteResult.getKeywordQuery(), keywordTopK, filter);

        // 4. 加权 RRF 融合
        List<Document> fusedDocs = rrfFusionService.fuse(vectorDocs, keywordDocs, finalTopK, rrfK,
                ragRetrievalProperties.getVectorWeight(), ragRetrievalProperties.getKeywordWeight());

        // 5. 重排（根据参数或配置决定）
        boolean doRerank = enableRerank != null ? enableRerank && ragRetrievalProperties.isRerankEnabled()
                : ragRetrievalProperties.isRerankEnabled();
        List<Document> finalDocs;
        if (doRerank) {
            finalDocs = executeRerank(fusedDocs, rewriteResult, finalTopK);
        } else {
            finalDocs = fusedDocs;
        }
        markMatchReason(finalDocs, rewriteResult);

        // 6. 构造结果（包含各阶段中间数据，供召回分析使用）
        Map<String, Object> retrievalMeta = buildRetrievalMeta(vectorDocs, keywordDocs, fusedDocs, finalDocs, complexQuery);
        log.info("[Retrieval] recall stats, vectorHits={}, keywordHits={}, fusedTopK={}, finalTopK={}, multiQuery={}, complexQuery={}",
                vectorDocs.size(), keywordDocs.size(), fusedDocs.size(), finalDocs.size(),
                ragRetrievalProperties.isMultiQueryEnabled(), complexQuery);

        RetrievalResult result = new RetrievalResult();
        result.setDocs(finalDocs);
        result.setVectorDocs(vectorDocs);
        result.setKeywordDocs(keywordDocs);
        result.setFusedDocs(fusedDocs);
        result.setRewriteQuery(rewriteResult.getKeywordQuery());
        result.setRewriteSemanticQuery(rewriteResult.getSemanticQuery());
        result.setRetrievalMeta(JSONUtil.toJsonStr(retrievalMeta));
        result.setRetrievalStrategy(doRerank
                ? RetrievalStrategyEnum.HYBRID_RRF_RERANK.getValue()
                : RetrievalStrategyEnum.HYBRID_RRF.getValue());
        return result;
    }

    /**
     * 简化调用：使用默认配置
     */
    public RetrievalResult retrieve(String question, Long knowledgeBaseId, Integer topK) {
        return retrieve(question, knowledgeBaseId, topK, null, null, List.of());
    }

    /**
     * 简化调用：带会话历史
     */
    public RetrievalResult retrieve(String question, Long knowledgeBaseId, Integer topK, List<Message> history) {
        return retrieve(question, knowledgeBaseId, topK, null, null, history);
    }

    /**
     * 兼容旧调用：不带会话历史
     */
    public RetrievalResult retrieve(String question, Long knowledgeBaseId, Integer topK,
                                    Double similarityThreshold, Boolean enableRerank) {
        return retrieve(question, knowledgeBaseId, topK, similarityThreshold, enableRerank, List.of());
    }

    // ==================== 内部方法 ====================

    private List<Document> executeVectorSearch(String originalQuestion, RewriteResult rewriteResult,
                                               Filter.Expression filterExpression, int vectorTopK, Double threshold) {
        boolean multiQuery = ragRetrievalProperties.isMultiQueryEnabled();

        Set<String> candidateQueries = new LinkedHashSet<>();
        String semanticQuery = StringUtils.trimToNull(rewriteResult.getSemanticQuery());
        if (semanticQuery != null) {
            candidateQueries.add(semanticQuery);
        }
        if (multiQuery) {
            if (ragRetrievalProperties.isIncludeOriginalQuery()) {
                String normalizedOriginalQuestion = StringUtils.trimToNull(originalQuestion);
                if (normalizedOriginalQuestion != null) {
                    candidateQueries.add(normalizedOriginalQuestion);
                }
            }
            if (CollUtil.isNotEmpty(rewriteResult.getSubQueries())) {
                for (String subQuery : rewriteResult.getSubQueries()) {
                    String normalizedSubQuery = StringUtils.trimToNull(subQuery);
                    if (normalizedSubQuery != null) {
                        candidateQueries.add(normalizedSubQuery);
                    }
                }
            }
        }

        List<Document> merged = new ArrayList<>();
        for (String candidateQuery : candidateQueries) {
            List<Document> docs = vectorStoreService.similaritySearch(
                    candidateQuery, filterExpression, vectorTopK, threshold);
            merged = mergeVectorResults(merged, docs, vectorTopK);
            if (!multiQuery) {
                break;
            }
        }
        if (shouldApplyRecallFallback(merged, threshold, vectorTopK)) {
            Double fallbackThreshold = ragRetrievalProperties.getFallbackSimilarityThreshold();
            if (fallbackThreshold != null && fallbackThreshold > 0 && threshold != null && fallbackThreshold >= threshold) {
                fallbackThreshold = null;
            }
            if (fallbackThreshold != null) {
                int i = 0;
                for (String candidateQuery : candidateQueries) {
                    if (i >= 2) {
                        break;
                    }
                    List<Document> fallbackDocs = vectorStoreService.similaritySearch(
                            candidateQuery, filterExpression, vectorTopK, fallbackThreshold);
                    merged = mergeVectorResults(merged, fallbackDocs, vectorTopK);
                    i++;
                }
            }
        }
        return merged;
    }

    private List<Document> mergeVectorResults(List<Document> docs1, List<Document> docs2, int topK) {
        Map<String, Document> merged = new LinkedHashMap<>();
        for (Document doc : docs1) {
            merged.put(ragDocumentHelper.buildDocKey(doc), doc);
        }
        for (Document doc : docs2) {
            String key = ragDocumentHelper.buildDocKey(doc);
            Document existing = merged.get(key);
            if (existing == null) {
                merged.put(key, doc);
                continue;
            }
            double existingScore = defaultScore(existing);
            double currentScore = defaultScore(doc);
            if (currentScore > existingScore) {
                ragDocumentHelper.mergeMetadata(doc, existing, "vector");
                merged.put(key, doc);
            } else {
                ragDocumentHelper.mergeMetadata(existing, doc, "vector");
            }
        }
        return merged.values().stream()
                .sorted(Comparator.comparingDouble(this::defaultScore).reversed())
                .limit(topK)
                .toList();
    }

    private RewriteResult buildRewriteResult(String question, List<Message> history) {
        if (!ragRetrievalProperties.isRewriteEnabled()) {
            RewriteResult result = new RewriteResult();
            result.setSemanticQuery(question);
            result.setKeywordQuery(question);
            result.setMustTerms(List.of());
            result.setMetadataFilters(Map.of());
            result.setSubQueries(List.of());
            return result;
        }
        return queryRewriteService.rewrite(question, history);
    }

    private List<Document> executeRerank(List<Document> fusedDocs, RewriteResult rewriteResult, int finalTopK) {
        int rerankTopN = Math.max(ragRetrievalProperties.getRerankTopN() <= 0 ? finalTopK
                : ragRetrievalProperties.getRerankTopN(), finalTopK);
        List<Document> candidates = fusedDocs.size() > rerankTopN ? fusedDocs.subList(0, rerankTopN) : fusedDocs;
        return rerankService.rerank(candidates, rewriteResult.getMustTerms(), rewriteResult.getMetadataFilters(), finalTopK);
    }

    private void markMatchReason(List<Document> docs, RewriteResult rewriteResult) {
        if (CollUtil.isEmpty(docs)) {
            return;
        }
        for (Document doc : docs) {
            Object rerankScore = doc.getMetadata().get("rerankScore");
            if (rerankScore != null) {
                doc.getMetadata().put("matchReason", MatchReasonEnum.RERANK.getValue());
                continue;
            }
            if (CollUtil.isNotEmpty(rewriteResult.getMustTerms())) {
                doc.getMetadata().put("matchReason", MatchReasonEnum.MUST_TERM.getValue());
            } else {
                doc.getMetadata().put("matchReason", MatchReasonEnum.HYBRID.getValue());
            }
        }
    }

    private Filter.Expression buildFilterExpression(Long knowledgeBaseId, Map<String, String> metadataFilters) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op op = null;
        if (knowledgeBaseId != null && knowledgeBaseId > 0) {
            op = b.eq("knowledgeBaseId", knowledgeBaseId);
        }
        if (CollUtil.isNotEmpty(metadataFilters)) {
            for (Map.Entry<String, String> entry : metadataFilters.entrySet()) {
                if (StringUtils.isNotBlank(entry.getValue())) {
                    FilterExpressionBuilder.Op next = b.eq(entry.getKey(), entry.getValue());
                    op = (op == null) ? next : b.and(op, next);
                }
            }
        }
        return op == null ? null : op.build();
    }

    private Map<String, Object> buildRetrievalMeta(List<Document> vectorDocs, List<Document> keywordDocs,
                                                     List<Document> fusedDocs, List<Document> finalDocs,
                                                     boolean complexQuery) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("vectorHitCount", vectorDocs.size());
        meta.put("keywordHitCount", keywordDocs.size());
        meta.put("fusedTopK", fusedDocs.size());
        meta.put("finalTopK", finalDocs.size());
        meta.put("complexQuery", complexQuery);
        meta.put("multiQueryEnabled", ragRetrievalProperties.isMultiQueryEnabled());
        meta.put("includeOriginalQuery", ragRetrievalProperties.isIncludeOriginalQuery());
        meta.put("recallFallbackEnabled", ragRetrievalProperties.isRecallFallbackEnabled());
        meta.put("recallFallbackMinHits", ragRetrievalProperties.getRecallFallbackMinHits());
        meta.put("fallbackSimilarityThreshold", ragRetrievalProperties.getFallbackSimilarityThreshold());
        meta.put("vectorWeight", ragRetrievalProperties.getVectorWeight());
        meta.put("keywordWeight", ragRetrievalProperties.getKeywordWeight());
        // 去重统计：向量和关键词的交叉命中数
        Set<String> vectorKeys = new HashSet<>();
        for (Document doc : vectorDocs) {
            vectorKeys.add(ragDocumentHelper.buildDocKey(doc));
        }
        long overlapCount = 0;
        for (Document doc : keywordDocs) {
            if (vectorKeys.contains(ragDocumentHelper.buildDocKey(doc))) {
                overlapCount++;
            }
        }
        meta.put("overlapCount", overlapCount);
        return meta;
    }

    private int resolveTopK(Integer topK, boolean complexQuery) {
        boolean explicitTopK = topK != null && topK > 0;
        int configuredTopK = explicitTopK ? topK : ragRetrievalProperties.getTopK();
        if (explicitTopK || !complexQuery) {
            return configuredTopK;
        }
        return Math.max(configuredTopK, ragRetrievalProperties.getComplexQueryTopK());
    }

    private boolean shouldApplyRecallFallback(List<Document> docs, Double threshold, int vectorTopK) {
        if (!ragRetrievalProperties.isRecallFallbackEnabled()) {
            return false;
        }
        if (threshold == null || threshold <= 0) {
            return false;
        }
        Double fallbackThreshold = ragRetrievalProperties.getFallbackSimilarityThreshold();
        if (fallbackThreshold == null || fallbackThreshold <= 0 || fallbackThreshold >= threshold) {
            return false;
        }
        int minHits = Math.max(1, Math.min(vectorTopK, ragRetrievalProperties.getRecallFallbackMinHits()));
        return docs.size() < minHits;
    }

    private double defaultScore(Document doc) {
        Double score = ragDocumentHelper.resolveVectorScore(doc);
        return score == null ? 0D : score;
    }

    private boolean isComplexQuery(String question) {
        if (StringUtils.isBlank(question)) {
            return false;
        }
        String normalizedQuestion = question.trim();
        return normalizedQuestion.length() >= 18
                || COMPLEX_QUERY_MARKERS.stream().anyMatch(normalizedQuestion::contains);
    }
}
