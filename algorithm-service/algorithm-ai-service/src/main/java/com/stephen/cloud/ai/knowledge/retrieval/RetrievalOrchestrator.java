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
 * 消除原来 RAGServiceImpl 中 ask/analyzeRecall 两处重复的检索逻辑。
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

    /**
     * 执行完整的检索编排
     *
     * @param question        用户原始问题
     * @param knowledgeBaseId 知识库ID
     * @param topK            最终返回数量（null 使用默认）
     * @param similarityThreshold 相似度阈值（null 使用默认）
     * @param enableRerank    是否启用重排（null 使用配置）
     * @return 检索结果（包含各阶段中间数据）
     */
    public RetrievalResult retrieve(String question, Long knowledgeBaseId, Integer topK,
                                     Double similarityThreshold, Boolean enableRerank) {
        int finalTopK = resolveTopK(topK);
        int vectorTopK = ragRetrievalProperties.getVectorTopK() <= 0 ? finalTopK : ragRetrievalProperties.getVectorTopK();
        int keywordTopK = ragRetrievalProperties.getKeywordTopK() <= 0 ? finalTopK : ragRetrievalProperties.getKeywordTopK();
        int rrfK = ragRetrievalProperties.getRrfK() <= 0 ? 60 : ragRetrievalProperties.getRrfK();
        Double threshold = similarityThreshold != null ? similarityThreshold : ragRetrievalProperties.getSimilarityThreshold();

        // 1. Query 改写
        RewriteResult rewriteResult = buildRewriteResult(question);
        Filter.Expression filter = buildFilterExpression(knowledgeBaseId, rewriteResult.getMetadataFilters());

        // 2. 向量检索（支持 Multi-Query 扩展）
        List<Document> vectorDocs = executeVectorSearch(question, rewriteResult, knowledgeBaseId, vectorTopK, threshold);

        // 3. 关键词 BM25 检索
        List<Document> keywordDocs = keywordSearchService.bm25Search(
                rewriteResult.getKeywordQuery(), knowledgeBaseId, keywordTopK, filter);

        // 4. 加权 RRF 融合
        List<Document> fusedDocs = rrfFusionService.fuse(vectorDocs, keywordDocs, finalTopK, rrfK,
                ragRetrievalProperties.getVectorWeight(), ragRetrievalProperties.getKeywordWeight());

        // 5. 重排（根据参数或配置决定）
        boolean doRerank = enableRerank != null ? enableRerank && ragRetrievalProperties.isRerankEnabled()
                : ragRetrievalProperties.isRerankEnabled();
        List<Document> finalDocs;
        if (doRerank) {
            finalDocs = executeRerank(fusedDocs, rewriteResult, finalTopK);
            markMatchReason(finalDocs, rewriteResult);
        } else {
            finalDocs = fusedDocs;
        }

        // 6. 构造结果（包含各阶段中间数据，供召回分析使用）
        Map<String, Object> retrievalMeta = buildRetrievalMeta(vectorDocs, keywordDocs, fusedDocs, finalDocs);
        log.info("[Retrieval] recall stats, vectorHits={}, keywordHits={}, fusedTopK={}, finalTopK={}, multiQuery={}",
                vectorDocs.size(), keywordDocs.size(), fusedDocs.size(), finalDocs.size(),
                ragRetrievalProperties.isMultiQueryEnabled());

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
        return retrieve(question, knowledgeBaseId, topK, null, null);
    }

    // ==================== 内部方法 ====================

    private List<Document> executeVectorSearch(String question, RewriteResult rewriteResult,
                                                Long knowledgeBaseId, int vectorTopK, Double threshold) {
        boolean multiQuery = ragRetrievalProperties.isMultiQueryEnabled();

        // Multi-Query 扩展：改写 query + 原始 query + LLM 子查询
        if (multiQuery && !question.equals(rewriteResult.getSemanticQuery())) {
            List<Document> semanticDocs = vectorStoreService.similaritySearch(
                    rewriteResult.getSemanticQuery(), knowledgeBaseId, vectorTopK, threshold);
            List<Document> rawDocs = vectorStoreService.similaritySearch(
                    question, knowledgeBaseId, vectorTopK, threshold);
            List<Document> merged = mergeVectorResults(semanticDocs, rawDocs, vectorTopK);

            // LLM 子查询扩展
            List<String> subQueries = rewriteResult.getSubQueries();
            if (CollUtil.isNotEmpty(subQueries)) {
                for (String subQuery : subQueries) {
                    if (StringUtils.isNotBlank(subQuery) && !subQuery.equals(question)) {
                        List<Document> subDocs = vectorStoreService.similaritySearch(
                                subQuery, knowledgeBaseId, vectorTopK, threshold);
                        merged = mergeVectorResults(merged, subDocs, vectorTopK);
                    }
                }
            }
            return merged;
        }

        return vectorStoreService.similaritySearch(
                rewriteResult.getSemanticQuery(), knowledgeBaseId, vectorTopK, threshold);
    }

    private List<Document> mergeVectorResults(List<Document> docs1, List<Document> docs2, int topK) {
        Map<String, Document> merged = new LinkedHashMap<>();
        for (Document doc : docs1) {
            merged.putIfAbsent(buildDocKey(doc), doc);
        }
        for (Document doc : docs2) {
            merged.putIfAbsent(buildDocKey(doc), doc);
        }
        return merged.values().stream().limit(topK).toList();
    }

    private String buildDocKey(Document doc) {
        Object documentId = doc.getMetadata().get("documentId");
        Object chunkIndex = doc.getMetadata().get("chunkIndex");
        if (documentId != null && chunkIndex != null) {
            return documentId + "_" + chunkIndex;
        }
        return doc.getId() != null ? doc.getId() : String.valueOf(doc.getText().hashCode());
    }

    private RewriteResult buildRewriteResult(String question) {
        if (!ragRetrievalProperties.isRewriteEnabled()) {
            RewriteResult result = new RewriteResult();
            result.setSemanticQuery(question);
            result.setKeywordQuery(question);
            result.setMustTerms(List.of());
            result.setMetadataFilters(Map.of());
            result.setSubQueries(List.of());
            return result;
        }
        return queryRewriteService.rewrite(question);
    }

    private List<Document> executeRerank(List<Document> fusedDocs, RewriteResult rewriteResult, int finalTopK) {
        int rerankTopN = ragRetrievalProperties.getRerankTopN() <= 0 ? finalTopK : ragRetrievalProperties.getRerankTopN();
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
                                                     List<Document> fusedDocs, List<Document> finalDocs) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("vectorHitCount", vectorDocs.size());
        meta.put("keywordHitCount", keywordDocs.size());
        meta.put("fusedTopK", fusedDocs.size());
        meta.put("finalTopK", finalDocs.size());
        meta.put("multiQueryEnabled", ragRetrievalProperties.isMultiQueryEnabled());
        meta.put("vectorWeight", ragRetrievalProperties.getVectorWeight());
        meta.put("keywordWeight", ragRetrievalProperties.getKeywordWeight());
        // 去重统计：向量和关键词的交叉命中数
        Set<String> vectorKeys = new HashSet<>();
        for (Document doc : vectorDocs) {
            vectorKeys.add(buildDocKey(doc));
        }
        long overlapCount = 0;
        for (Document doc : keywordDocs) {
            if (vectorKeys.contains(buildDocKey(doc))) {
                overlapCount++;
            }
        }
        meta.put("overlapCount", overlapCount);
        return meta;
    }

    private int resolveTopK(Integer topK) {
        return topK == null || topK <= 0 ? ragRetrievalProperties.getTopK() : topK;
    }
}
