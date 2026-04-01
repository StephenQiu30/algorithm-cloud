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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.*;


import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 检索编排器
 * <p>
 * 统一编排 RAG 检索的完整链路：
 * Query改写 → Multi-Query向量检索（并行） → 关键词BM25检索（并行） → 补召回 → 加权RRF融合 → Rerank → 标记命中原因
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

    @Resource
    @Qualifier("aiAsyncExecutor")
    private Executor aiAsyncExecutor;

    /**
     * 执行完整的检索编排
     *
     * @param question            用户原始问题
     * @param knowledgeBaseId     知识库ID
     * @param topK                最终返回数量（null 使用默认）
     * @param similarityThreshold 相似度阈值（null 使用默认）
     * @param enableRerank        是否启用重排（null 使用配置）
     * @param history             会话历史（用于多轮问题压缩检索）
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

        // 2. 向量检索 + 关键词 BM25 检索（并行执行 + 超时保护）
        int timeoutSeconds = ragRetrievalProperties.getRetrievalTimeoutSeconds() <= 0
                ? 3 : ragRetrievalProperties.getRetrievalTimeoutSeconds();

        CompletableFuture<List<Document>> vectorFuture = CompletableFuture
                .supplyAsync(() -> executeVectorSearch(question, rewriteResult, filter, vectorTopK, threshold), aiAsyncExecutor)
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.warn("[Retrieval] 向量检索超时或异常, 降级为空结果, error={}", ex.getMessage());
                    return List.of();
                });

        CompletableFuture<List<Document>> keywordFuture = CompletableFuture
                .supplyAsync(() -> executeKeywordSearch(question, rewriteResult, filter, keywordTopK), aiAsyncExecutor)
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.warn("[Retrieval] 关键词检索超时或异常, 降级为空结果, error={}", ex.getMessage());
                    return List.of();
                });

        List<Document> vectorDocs = vectorFuture.join();
        List<Document> keywordDocs = keywordFuture.join();

        // 3. 低召回补召回：当向量命中不足且配置开启时，放宽阈值二次召回
        if (ragRetrievalProperties.isRecallFallbackEnabled()
                && vectorDocs.size() < ragRetrievalProperties.getRecallFallbackMinHits()) {
            Double fallbackThreshold = ragRetrievalProperties.getFallbackSimilarityThreshold();
            List<Document> supplementDocs = vectorStoreService.similaritySearch(
                    rewriteResult.getSemanticQuery(), filter, vectorTopK, fallbackThreshold);
            vectorDocs = mergeWithDedup(vectorDocs, supplementDocs);
            log.info("[Retrieval] 触发补召回, originalHits={}, supplementHits={}, mergedHits={}",
                    vectorFuture.join().size(), supplementDocs.size(), vectorDocs.size());
        }

        // 4. 加权 RRF 融合
        boolean doRerank = enableRerank != null ? enableRerank && ragRetrievalProperties.isRerankEnabled()
                : ragRetrievalProperties.isRerankEnabled();
        int fuseTopK = finalTopK;
        if (doRerank) {
            int rerankTopN = Math.max(ragRetrievalProperties.getRerankTopN() <= 0 ? finalTopK
                    : ragRetrievalProperties.getRerankTopN(), finalTopK);
            fuseTopK = Math.max(finalTopK, rerankTopN);
        }
        List<Document> fusedDocs = rrfFusionService.fuse(vectorDocs, keywordDocs, fuseTopK, rrfK,
                ragRetrievalProperties.getVectorWeight(), ragRetrievalProperties.getKeywordWeight());

        // 5. 重排（根据参数或配置决定）
        List<Document> finalDocs;
        if (doRerank) {
            finalDocs = executeRerank(fusedDocs, question, rewriteResult, finalTopK);
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

    /**
     * 向量检索：支持 Multi-Query 扩展召回
     */
    private List<Document> executeVectorSearch(String originalQuestion, RewriteResult rewriteResult,
                                               Filter.Expression filterExpression, int vectorTopK, Double threshold) {
        String semanticQuery = StringUtils.trimToNull(rewriteResult.getSemanticQuery());
        String primaryQuery = semanticQuery == null ? StringUtils.trimToNull(originalQuestion) : semanticQuery;
        if (primaryQuery == null) {
            return List.of();
        }

        // 主查询
        List<Document> results = new ArrayList<>(
                vectorStoreService.similaritySearch(primaryQuery, filterExpression, vectorTopK, threshold));

        // Multi-Query 扩展召回：对每个子查询执行向量检索，合并去重
        if (ragRetrievalProperties.isMultiQueryEnabled()
                && CollUtil.isNotEmpty(rewriteResult.getSubQueries())) {
            Set<String> seenKeys = results.stream()
                    .map(ragDocumentHelper::buildDocKey)
                    .collect(Collectors.toCollection(HashSet::new));
            for (String subQuery : rewriteResult.getSubQueries()) {
                if (StringUtils.isBlank(subQuery)) {
                    continue;
                }
                List<Document> subResults = vectorStoreService.similaritySearch(
                        subQuery, filterExpression, vectorTopK, threshold);
                for (Document doc : subResults) {
                    if (seenKeys.add(ragDocumentHelper.buildDocKey(doc))) {
                        results.add(doc);
                    }
                }
            }
            log.info("[Retrieval] Multi-Query 扩展完成, subQueryCount={}, totalVectorHits={}",
                    rewriteResult.getSubQueries().size(), results.size());
        }

        // 是否同时保留原始问题的召回结果
        if (ragRetrievalProperties.isMultiQueryEnabled()
                && ragRetrievalProperties.isIncludeOriginalQuery()
                && semanticQuery != null && !semanticQuery.equals(originalQuestion)) {
            Set<String> seenKeys = results.stream()
                    .map(ragDocumentHelper::buildDocKey)
                    .collect(Collectors.toCollection(HashSet::new));
            List<Document> originalResults = vectorStoreService.similaritySearch(
                    originalQuestion, filterExpression, vectorTopK, threshold);
            for (Document doc : originalResults) {
                if (seenKeys.add(ragDocumentHelper.buildDocKey(doc))) {
                    results.add(doc);
                }
            }
        }

        return results;
    }

    private List<Document> executeKeywordSearch(String originalQuestion, RewriteResult rewriteResult,
                                                Filter.Expression filterExpression, int keywordTopK) {
        String keywordQuery = StringUtils.trimToNull(rewriteResult.getKeywordQuery());
        if (keywordQuery == null) {
            keywordQuery = StringUtils.trimToNull(originalQuestion);
        }
        if (keywordQuery == null) {
            return List.of();
        }
        return keywordSearchService.bm25Search(keywordQuery, keywordTopK, filterExpression);
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

    private List<Document> executeRerank(List<Document> fusedDocs, String originalQuestion,
                                          RewriteResult rewriteResult, int finalTopK) {
        int rerankTopN = Math.max(ragRetrievalProperties.getRerankTopN() <= 0 ? finalTopK
                : ragRetrievalProperties.getRerankTopN(), finalTopK);
        List<Document> candidates = fusedDocs.size() > rerankTopN ? fusedDocs.subList(0, rerankTopN) : fusedDocs;
        return rerankService.rerank(candidates, originalQuestion, rewriteResult.getMustTerms(),
                rewriteResult.getMetadataFilters(), finalTopK);
    }

    private void markMatchReason(List<Document> docs, RewriteResult rewriteResult) {
        if (CollUtil.isEmpty(docs)) {
            return;
        }
        for (Document doc : docs) {
            Object rerankScore = doc.getMetadata().get(RERANK_SCORE);
            if (rerankScore != null) {
                doc.getMetadata().put(MATCH_REASON, MatchReasonEnum.RERANK.getValue());
                continue;
            }
            if (CollUtil.isNotEmpty(rewriteResult.getMustTerms())) {
                doc.getMetadata().put(MATCH_REASON, MatchReasonEnum.MUST_TERM.getValue());
            } else {
                doc.getMetadata().put(MATCH_REASON, MatchReasonEnum.HYBRID.getValue());
            }
        }
    }

    private Filter.Expression buildFilterExpression(Long knowledgeBaseId, Map<String, String> metadataFilters) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op op = null;
        if (knowledgeBaseId != null && knowledgeBaseId > 0) {
            op = b.eq(KNOWLEDGE_BASE_ID, knowledgeBaseId);
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

    /**
     * 合并两个文档列表并去重
     */
    private List<Document> mergeWithDedup(List<Document> primary, List<Document> supplement) {
        if (CollUtil.isEmpty(supplement)) {
            return primary;
        }
        List<Document> merged = new ArrayList<>(primary);
        Set<String> seenKeys = primary.stream()
                .map(ragDocumentHelper::buildDocKey)
                .collect(Collectors.toCollection(HashSet::new));
        for (Document doc : supplement) {
            if (seenKeys.add(ragDocumentHelper.buildDocKey(doc))) {
                merged.add(doc);
            }
        }
        return merged;
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
        meta.put("vectorWeight", ragRetrievalProperties.getVectorWeight());
        meta.put("keywordWeight", ragRetrievalProperties.getKeywordWeight());
        meta.put("recallFallbackEnabled", ragRetrievalProperties.isRecallFallbackEnabled());
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

    /**
     * 复杂查询判断（阈值和关键词均从配置读取）
     */
    private boolean isComplexQuery(String question) {
        if (StringUtils.isBlank(question)) {
            return false;
        }
        String normalizedQuestion = question.trim();
        int minLength = ragRetrievalProperties.getComplexQueryMinLength() <= 0
                ? 18 : ragRetrievalProperties.getComplexQueryMinLength();
        if (normalizedQuestion.length() >= minLength) {
            return true;
        }
        List<String> markers = ragRetrievalProperties.getComplexQueryMarkers();
        if (CollUtil.isEmpty(markers)) {
            return false;
        }
        return markers.stream().anyMatch(normalizedQuestion::contains);
    }
}
