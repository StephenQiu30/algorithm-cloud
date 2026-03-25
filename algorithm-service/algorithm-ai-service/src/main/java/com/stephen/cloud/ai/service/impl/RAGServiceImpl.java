package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.config.RagRetrievalProperties;
import com.stephen.cloud.ai.convert.RAGConvert;
import com.stephen.cloud.ai.knowledge.retrieval.model.RetrievalResult;
import com.stephen.cloud.ai.knowledge.rerank.RerankService;
import com.stephen.cloud.ai.knowledge.retrieval.RRFFusionService;
import com.stephen.cloud.ai.knowledge.rewrite.QueryRewriteService;
import com.stephen.cloud.ai.knowledge.rewrite.RewriteResult;
import com.stephen.cloud.ai.mapper.RAGHistoryMapper;
import com.stephen.cloud.ai.model.entity.RAGHistory;
import com.stephen.cloud.api.ai.model.enums.MatchReasonEnum;
import com.stephen.cloud.api.ai.model.enums.RetrievalStrategyEnum;
import com.stephen.cloud.ai.service.KeywordSearchService;
import com.stephen.cloud.ai.service.RAGService;
import com.stephen.cloud.ai.service.VectorStoreService;
import com.stephen.cloud.api.ai.model.dto.rag.BatchRecallRequest;
import com.stephen.cloud.api.ai.model.dto.rag.RecallAnalysisRequest;
import com.stephen.cloud.api.ai.model.dto.rag.RecallTestItem;
import com.stephen.cloud.api.ai.model.vo.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RAGServiceImpl implements RAGService {

    @Resource
    private ChatClient chatClient;

    @Resource
    private RAGHistoryMapper ragHistoryMapper;

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

    @Override
    public RAGAnswerVO ask(String question, Long knowledgeBaseId, Long userId, Integer topK) {
        long start = System.currentTimeMillis();
        RetrievalResult retrievalResult = retrieveWithHybrid(question, knowledgeBaseId, topK);
        List<Document> docs = retrievalResult.getDocs();
        String context = buildContext(docs);
        String prompt = buildPrompt(question, context);
        String answer = chatClient.prompt().user(prompt).call().content();
        List<SourceVO> sources = buildSources(docs);
        long responseTime = System.currentTimeMillis() - start;
        saveHistory(question, answer, knowledgeBaseId, userId, JSONUtil.toJsonStr(sources), responseTime,
                retrievalResult.getRewriteQuery(), retrievalResult.getRetrievalMeta(), retrievalResult.getRetrievalStrategy());
        RAGAnswerVO vo = new RAGAnswerVO();
        vo.setAnswer(answer);
        vo.setSources(sources);
        vo.setResponseTime(responseTime);
        return vo;
    }

    @Override
    public Flux<String> askStream(String question, Long knowledgeBaseId, Long userId, Integer topK) {
        return Flux.defer(() -> {
            long start = System.currentTimeMillis();
            RetrievalResult retrievalResult = retrieveWithHybrid(question, knowledgeBaseId, topK);
            List<Document> docs = retrievalResult.getDocs();
            String context = buildContext(docs);
            String prompt = buildPrompt(question, context);
            List<SourceVO> sources = buildSources(docs);
            StringBuilder answerBuilder = new StringBuilder();
            return chatClient.prompt().user(prompt).stream().content()
                    .doOnNext(answerBuilder::append)
                    .doOnComplete(() -> {
                        long responseTime = System.currentTimeMillis() - start;
                        saveHistory(question, answerBuilder.toString(), knowledgeBaseId, userId, JSONUtil.toJsonStr(sources), responseTime,
                                retrievalResult.getRewriteQuery(), retrievalResult.getRetrievalMeta(), retrievalResult.getRetrievalStrategy());
                    });
        });
    }

    @Override
    public void saveHistory(String question, String answer, Long knowledgeBaseId, Long userId, String sources, Long responseTime) {
        saveHistory(question, answer, knowledgeBaseId, userId, sources, responseTime, null, null, null);
    }

    private void saveHistory(String question, String answer, Long knowledgeBaseId, Long userId, String sources, Long responseTime,
                             String rewriteQuery, String retrievalMeta, String retrievalStrategy) {
        RAGHistory ragHistory = new RAGHistory();
        ragHistory.setQuestion(question);
        ragHistory.setAnswer(answer);
        ragHistory.setKnowledgeBaseId(knowledgeBaseId);
        ragHistory.setUserId(userId);
        ragHistory.setSources(sources);
        ragHistory.setResponseTime(responseTime);
        ragHistory.setRewriteQuery(rewriteQuery);
        ragHistory.setRetrievalMeta(retrievalMeta);
        ragHistory.setRetrievalStrategy(retrievalStrategy);
        ragHistoryMapper.insert(ragHistory);
    }

    @Override
    public Page<RAGHistoryVO> listHistoryByPage(long current, long size, Long knowledgeBaseId, Long userId) {
        LambdaQueryWrapper<RAGHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(knowledgeBaseId != null && knowledgeBaseId > 0, RAGHistory::getKnowledgeBaseId, knowledgeBaseId)
                .eq(userId != null && userId > 0, RAGHistory::getUserId, userId)
                .orderByDesc(RAGHistory::getCreateTime);
        Page<RAGHistory> page = new Page<>(current, size);
        Page<RAGHistory> historyPage = ragHistoryMapper.selectPage(page, queryWrapper);
        Page<RAGHistoryVO> voPage = new Page<>(historyPage.getCurrent(), historyPage.getSize(), historyPage.getTotal());
        List<RAGHistoryVO> voList = historyPage.getRecords().stream().map(item -> {
            RAGHistoryVO vo = RAGConvert.INSTANCE.objToVo(item);
            String sourceJson = item.getSources();
            if (StringUtils.isNotBlank(sourceJson)) {
                vo.setSources(JSONUtil.toList(sourceJson, SourceVO.class));
            }
            return vo;
        }).toList();
        voPage.setRecords(voList);
        return voPage;
    }

    private String buildContext(List<Document> docs) {
        if (CollUtil.isEmpty(docs)) {
            return "";
        }
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            context.append("片段").append(i + 1).append(":\n").append(doc.getText()).append("\n\n");
        }
        return context.toString();
    }

    private String buildPrompt(String question, String context) {
        if (StringUtils.isBlank(context)) {
            return "你是知识库问答助手。用户问题是：" + question + "。当前知识库没有检索到相关内容，请明确告知用户。";
        }
        return "你是知识库问答助手。请基于以下上下文回答问题，若上下文不足请明确说明。\n\n上下文:\n"
                + context + "\n问题:\n" + question;
    }

    private List<SourceVO> buildSources(List<Document> docs) {
        List<SourceVO> sourceList = new ArrayList<>();
        if (CollUtil.isEmpty(docs)) {
            return sourceList;
        }
        for (Document doc : docs) {
            SourceVO sourceVO = new SourceVO();
            Object documentId = doc.getMetadata().get("documentId");
            Object documentName = doc.getMetadata().get("documentName");
            Object chunkIndex = doc.getMetadata().get("chunkIndex");
            Object score = doc.getMetadata().get("distance");
            if (documentId != null) {
                sourceVO.setDocumentId(Long.valueOf(String.valueOf(documentId)));
            }
            sourceVO.setDocumentName(documentName == null ? null : String.valueOf(documentName));
            if (chunkIndex != null) {
                sourceVO.setChunkIndex(Integer.valueOf(String.valueOf(chunkIndex)));
            }
            sourceVO.setSourceType(toStringValue(doc.getMetadata().get("sourceType")));
            sourceVO.setVersion(toStringValue(doc.getMetadata().get("version")));
            sourceVO.setBizTag(toStringValue(doc.getMetadata().get("bizTag")));
            sourceVO.setMatchReason(toStringValue(doc.getMetadata().get("matchReason")));
            sourceVO.setContent(doc.getText());
            if (score != null) {
                sourceVO.setScore(Double.valueOf(String.valueOf(score)));
            }
            // 相似度详情
            sourceVO.setVectorSimilarity(toDoubleValue(doc.getMetadata().get("distance")));
            sourceVO.setKeywordRelevance(toDoubleValue(doc.getMetadata().get("keywordScore")));
            sourceList.add(sourceVO);
        }
        return sourceList;
    }

    private RetrievalResult retrieveWithHybrid(String question, Long knowledgeBaseId, Integer topK) {
        int finalTopK = topK == null || topK <= 0 ? ragRetrievalProperties.getTopK() : topK;
        int vectorTopK = ragRetrievalProperties.getVectorTopK() <= 0 ? finalTopK : ragRetrievalProperties.getVectorTopK();
        int keywordTopK = ragRetrievalProperties.getKeywordTopK() <= 0 ? finalTopK : ragRetrievalProperties.getKeywordTopK();
        int rrfK = ragRetrievalProperties.getRrfK() <= 0 ? 60 : ragRetrievalProperties.getRrfK();
        RewriteResult rewriteResult = buildRewriteResult(question);
        Filter.Expression filter = buildFilterExpression(knowledgeBaseId, rewriteResult.getMetadataFilters());

        // Multi-Query 扩展：使用改写后的语义 query + 原始 query 进行双路向量检索
        List<Document> vectorDocs;
        if (ragRetrievalProperties.isMultiQueryEnabled()
                && !question.equals(rewriteResult.getSemanticQuery())) {
            List<Document> semanticDocs = vectorStoreService.similaritySearch(
                    rewriteResult.getSemanticQuery(), knowledgeBaseId, vectorTopK, ragRetrievalProperties.getSimilarityThreshold());
            List<Document> rawDocs = vectorStoreService.similaritySearch(
                    question, knowledgeBaseId, vectorTopK, ragRetrievalProperties.getSimilarityThreshold());
            vectorDocs = mergeVectorResults(semanticDocs, rawDocs, vectorTopK);
        } else {
            vectorDocs = vectorStoreService.similaritySearch(
                    rewriteResult.getSemanticQuery(), knowledgeBaseId, vectorTopK, ragRetrievalProperties.getSimilarityThreshold());
        }

        List<Document> keywordDocs = keywordSearchService.bm25Search(
                rewriteResult.getKeywordQuery(), knowledgeBaseId, keywordTopK, filter);

        // 加权 RRF 融合
        List<Document> fusedDocs = rrfFusionService.fuse(vectorDocs, keywordDocs, finalTopK, rrfK,
                ragRetrievalProperties.getVectorWeight(), ragRetrievalProperties.getKeywordWeight());
        List<Document> rerankedDocs = maybeRerank(fusedDocs, rewriteResult, finalTopK);
        markMatchReason(rerankedDocs, rewriteResult);
        Map<String, Object> retrievalMeta = new HashMap<>();
        retrievalMeta.put("vectorHitCount", vectorDocs.size());
        retrievalMeta.put("keywordHitCount", keywordDocs.size());
        retrievalMeta.put("fusedTopK", fusedDocs.size());
        retrievalMeta.put("finalTopK", rerankedDocs.size());
        retrievalMeta.put("multiQueryEnabled", ragRetrievalProperties.isMultiQueryEnabled());
        retrievalMeta.put("vectorWeight", ragRetrievalProperties.getVectorWeight());
        retrievalMeta.put("keywordWeight", ragRetrievalProperties.getKeywordWeight());
        log.info("[RAG] recall stats, vectorHits={}, keywordHits={}, fusedTopK={}, finalTopK={}, multiQuery={}",
                vectorDocs.size(), keywordDocs.size(), fusedDocs.size(), rerankedDocs.size(),
                ragRetrievalProperties.isMultiQueryEnabled());
        RetrievalResult result = new RetrievalResult();
        result.setDocs(rerankedDocs);
        result.setRewriteQuery(rewriteResult.getKeywordQuery());
        result.setRetrievalMeta(JSONUtil.toJsonStr(retrievalMeta));
        result.setRetrievalStrategy(ragRetrievalProperties.isRerankEnabled()
                ? RetrievalStrategyEnum.HYBRID_RRF_RERANK.getValue()
                : RetrievalStrategyEnum.HYBRID_RRF.getValue());
        return result;
    }

    /**
     * 合并多路向量检索结果，去重保留最高分
     */
    private List<Document> mergeVectorResults(List<Document> docs1, List<Document> docs2, int topK) {
        Map<String, Document> merged = new LinkedHashMap<>();
        for (Document doc : docs1) {
            String key = buildDocKey(doc);
            merged.putIfAbsent(key, doc);
        }
        for (Document doc : docs2) {
            String key = buildDocKey(doc);
            merged.putIfAbsent(key, doc);
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
            RewriteResult rewriteResult = new RewriteResult();
            rewriteResult.setSemanticQuery(question);
            rewriteResult.setKeywordQuery(question);
            rewriteResult.setMustTerms(List.of());
            rewriteResult.setMetadataFilters(Map.of());
            return rewriteResult;
        }
        return queryRewriteService.rewrite(question);
    }

    private List<Document> maybeRerank(List<Document> fusedDocs, RewriteResult rewriteResult, int finalTopK) {
        if (!ragRetrievalProperties.isRerankEnabled()) {
            return fusedDocs;
        }
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

    @Override
    public RecallAnalysisVO analyzeRecall(RecallAnalysisRequest request) {
        long start = System.currentTimeMillis();
        String question = request.getQuestion();
        Long knowledgeBaseId = request.getKnowledgeBaseId();
        int finalTopK = request.getTopK() == null || request.getTopK() <= 0 ? ragRetrievalProperties.getTopK() : request.getTopK();
        Double threshold = request.getSimilarityThreshold() == null ? ragRetrievalProperties.getSimilarityThreshold() : request.getSimilarityThreshold();

        // 1. 改写
        RewriteResult rewriteResult = buildRewriteResult(question);
        Filter.Expression filter = buildFilterExpression(knowledgeBaseId, rewriteResult.getMetadataFilters());

        // 2. 向量检索（支持 Multi-Query）
        List<Document> vectorDocs;
        if (ragRetrievalProperties.isMultiQueryEnabled()
                && !question.equals(rewriteResult.getSemanticQuery())) {
            List<Document> semanticDocs = vectorStoreService.similaritySearch(
                    rewriteResult.getSemanticQuery(), knowledgeBaseId, finalTopK, threshold);
            List<Document> rawDocs = vectorStoreService.similaritySearch(
                    question, knowledgeBaseId, finalTopK, threshold);
            vectorDocs = mergeVectorResults(semanticDocs, rawDocs, finalTopK);
        } else {
            vectorDocs = vectorStoreService.similaritySearch(
                    rewriteResult.getSemanticQuery(), knowledgeBaseId, finalTopK, threshold);
        }

        // 3. 关键词检索
        List<Document> keywordDocs = keywordSearchService.bm25Search(
                rewriteResult.getKeywordQuery(), knowledgeBaseId, finalTopK, filter);

        // 4. 加权融合
        int rrfK = ragRetrievalProperties.getRrfK() <= 0 ? 60 : ragRetrievalProperties.getRrfK();
        List<Document> fusedDocs = rrfFusionService.fuse(vectorDocs, keywordDocs, finalTopK, rrfK,
                ragRetrievalProperties.getVectorWeight(), ragRetrievalProperties.getKeywordWeight());

        // 5. 重排
        List<Document> finalDocs;
        if (Boolean.TRUE.equals(request.getEnableRerank()) && ragRetrievalProperties.isRerankEnabled()) {
            finalDocs = maybeRerank(fusedDocs, rewriteResult, finalTopK);
            markMatchReason(finalDocs, rewriteResult);
        } else {
            finalDocs = fusedDocs;
        }

        // 6. 构造 VO（含相似度统计）
        List<RetrievalHitVO> finalHitVOs = convertToHitVOs(finalDocs);
        RecallAnalysisVO vo = new RecallAnalysisVO();
        vo.setQuestion(question);
        vo.setVectorHits(convertToHitVOs(vectorDocs));
        vo.setKeywordHits(convertToHitVOs(keywordDocs));
        vo.setFusedHits(convertToHitVOs(fusedDocs));
        vo.setFinalResults(finalHitVOs);
        vo.setCostMs(System.currentTimeMillis() - start);
        vo.setRewriteQuery(rewriteResult.getSemanticQuery());
        vo.setRetrievalStrategy(ragRetrievalProperties.isRerankEnabled()
                ? RetrievalStrategyEnum.HYBRID_RRF_RERANK.getValue()
                : RetrievalStrategyEnum.HYBRID_RRF.getValue());
        // 统计相似度
        computeSimilarityStats(finalHitVOs, vo);
        return vo;
    }

    private void computeSimilarityStats(List<RetrievalHitVO> hits, RecallAnalysisVO vo) {
        if (CollUtil.isEmpty(hits)) {
            return;
        }
        double sum = 0;
        double max = 0;
        int count = 0;
        for (RetrievalHitVO hit : hits) {
            Double sim = hit.getSimilarityScore() != null ? hit.getSimilarityScore() : hit.getVectorScore();
            if (sim != null) {
                sum += sim;
                max = Math.max(max, sim);
                count++;
            }
        }
        if (count > 0) {
            vo.setAvgSimilarity(sum / count);
            vo.setMaxSimilarity(max);
        }
    }

    @Override
    public BatchRecallVO batchAnalyzeRecall(BatchRecallRequest request) {
        RecallAnalysisRequest config = request.getConfig();
        List<RecallTestItem> items = request.getItems();
        if (CollUtil.isEmpty(items)) {
            return new BatchRecallVO();
        }

        List<RecallItemResultVO> results = new ArrayList<>();
        double totalHitRate = 0;
        double totalRecall = 0;
        int testCountWithExpectation = 0;

        for (RecallTestItem item : items) {
            RecallAnalysisRequest itemRequest = new RecallAnalysisRequest();
            itemRequest.setQuestion(item.getQuestion());
            itemRequest.setKnowledgeBaseId(config.getKnowledgeBaseId());
            itemRequest.setTopK(config.getTopK());
            itemRequest.setSimilarityThreshold(config.getSimilarityThreshold());
            itemRequest.setEnableRerank(config.getEnableRerank());

            RecallAnalysisVO analysis = analyzeRecall(itemRequest);
            List<RetrievalHitVO> finalResults = analysis.getFinalResults();
            List<String> recalledIds = finalResults.stream().map(RetrievalHitVO::getId).toList();

            RecallItemResultVO itemResult = new RecallItemResultVO();
            itemResult.setQuestion(item.getQuestion());
            itemResult.setRetrievedChunks(finalResults);

            List<String> expectedIds = item.getExpectedChunkIds();
            if (CollUtil.isNotEmpty(expectedIds)) {
                testCountWithExpectation++;
                long hitCount = expectedIds.stream().filter(recalledIds::contains).count();
                itemResult.setIsHit(hitCount > 0);
                itemResult.setRecall(hitCount * 1.0D / expectedIds.size());

                totalHitRate += (hitCount > 0 ? 1 : 0);
                totalRecall += itemResult.getRecall();
            }
            // 统计相似度
            if (CollUtil.isNotEmpty(finalResults)) {
                double simSum = 0;
                double simMax = 0;
                int simCount = 0;
                for (RetrievalHitVO hit : finalResults) {
                    Double sim = hit.getSimilarityScore() != null ? hit.getSimilarityScore() : hit.getVectorScore();
                    if (sim != null) {
                        simSum += sim;
                        simMax = Math.max(simMax, sim);
                        simCount++;
                    }
                }
                if (simCount > 0) {
                    itemResult.setAvgSimilarity(simSum / simCount);
                    itemResult.setMaxSimilarity(simMax);
                }
            }
            results.add(itemResult);
        }

        BatchRecallVO vo = new BatchRecallVO();
        vo.setItemResults(results);
        if (testCountWithExpectation > 0) {
            vo.setOverallHitRate(totalHitRate / testCountWithExpectation);
            vo.setMeanRecall(totalRecall / testCountWithExpectation);
        }
        return vo;
    }

    private List<RetrievalHitVO> convertToHitVOs(List<Document> docs) {
        if (CollUtil.isEmpty(docs)) {
            return new ArrayList<>();
        }
        return docs.stream().map(doc -> {
            RetrievalHitVO hit = new RetrievalHitVO();
            hit.setId(doc.getId());
            hit.setContent(doc.getText());
            hit.setDocumentName(toStringValue(doc.getMetadata().get("documentName")));
            hit.setChunkIndex(toIntegerValue(doc.getMetadata().get("chunkIndex")));
            hit.setVectorScore(toDoubleValue(doc.getMetadata().get("distance")));
            hit.setKeywordScore(toDoubleValue(doc.getMetadata().get("keywordScore")));
            hit.setFusionScore(toDoubleValue(doc.getMetadata().get("fusionScore")));
            hit.setScore(toDoubleValue(doc.getMetadata().get("score")));
            hit.setSimilarityScore(toDoubleValue(doc.getMetadata().get("distance")));
            hit.setRerankScore(toDoubleValue(doc.getMetadata().get("rerankScore")));
            hit.setMatchReason(toStringValue(doc.getMetadata().get("matchReason")));
            return hit;
        }).toList();
    }

    private Double toDoubleValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toIntegerValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
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

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

}
