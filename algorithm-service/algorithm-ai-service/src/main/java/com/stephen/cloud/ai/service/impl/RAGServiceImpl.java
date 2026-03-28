package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.convert.RAGConvert;
import com.stephen.cloud.ai.knowledge.retrieval.RetrievalOrchestrator;
import com.stephen.cloud.ai.knowledge.retrieval.model.RetrievalResult;
import com.stephen.cloud.ai.mapper.RAGHistoryMapper;
import com.stephen.cloud.ai.model.entity.RAGHistory;
import com.stephen.cloud.ai.service.RAGService;
import com.stephen.cloud.api.ai.model.dto.rag.BatchRecallRequest;
import com.stephen.cloud.api.ai.model.dto.rag.RAGHistoryQueryRequest;
import com.stephen.cloud.api.ai.model.dto.rag.RecallAnalysisRequest;
import com.stephen.cloud.api.ai.model.dto.rag.RecallTestItem;
import com.stephen.cloud.api.ai.model.vo.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RAG 服务实现
 * <p>
 * 核心职责：问答、流式问答、召回分析、批量召回测试。
 * 检索编排逻辑已提取到 {@link RetrievalOrchestrator}，本类专注于上层业务。
 * </p>
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class RAGServiceImpl implements RAGService {

    @Resource
    private ChatClient chatClient;

    @Resource
    private RAGHistoryMapper ragHistoryMapper;

    @Resource
    private RetrievalOrchestrator retrievalOrchestrator;

    @Override
    public RAGAnswerVO ask(String question, Long knowledgeBaseId, Long userId, Integer topK) {
        long start = System.currentTimeMillis();
        RetrievalResult result = retrievalOrchestrator.retrieve(question, knowledgeBaseId, topK);
        List<Document> docs = result.getDocs();
        String context = buildContext(docs);
        String prompt = buildPrompt(question, context);
        String answer = chatClient.prompt().user(prompt).call().content();
        List<SourceVO> sources = buildSources(docs);
        long responseTime = System.currentTimeMillis() - start;
        saveHistory(question, answer, knowledgeBaseId, userId, JSONUtil.toJsonStr(sources), responseTime,
                result.getRewriteQuery(), result.getRetrievalMeta(), result.getRetrievalStrategy());
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
            RetrievalResult result = retrievalOrchestrator.retrieve(question, knowledgeBaseId, topK);
            List<Document> docs = result.getDocs();
            String context = buildContext(docs);
            String prompt = buildPrompt(question, context);
            List<SourceVO> sources = buildSources(docs);
            StringBuilder answerBuilder = new StringBuilder();
            return chatClient.prompt().user(prompt).stream().content()
                    .doOnNext(answerBuilder::append)
                    .doOnComplete(() -> {
                        long responseTime = System.currentTimeMillis() - start;
                        saveHistory(question, answerBuilder.toString(), knowledgeBaseId, userId,
                                JSONUtil.toJsonStr(sources), responseTime,
                                result.getRewriteQuery(), result.getRetrievalMeta(), result.getRetrievalStrategy());
                    });
        });
    }

    @Override
    public RecallAnalysisVO analyzeRecall(RecallAnalysisRequest request) {
        long start = System.currentTimeMillis();
        RetrievalResult result = retrievalOrchestrator.retrieve(
                request.getQuestion(), request.getKnowledgeBaseId(), request.getTopK(),
                request.getSimilarityThreshold(), request.getEnableRerank());

        // 构造 VO（利用 RetrievalResult 中的各阶段数据）
        List<RetrievalHitVO> finalHitVOs = convertToHitVOs(result.getDocs());
        RecallAnalysisVO vo = new RecallAnalysisVO();
        vo.setQuestion(request.getQuestion());
        vo.setVectorHits(convertToHitVOs(result.getVectorDocs()));
        vo.setKeywordHits(convertToHitVOs(result.getKeywordDocs()));
        vo.setFusedHits(convertToHitVOs(result.getFusedDocs()));
        vo.setFinalResults(finalHitVOs);
        vo.setCostMs(System.currentTimeMillis() - start);
        vo.setRewriteQuery(result.getRewriteSemanticQuery());
        vo.setRewriteKeywordQuery(result.getRewriteQuery());
        vo.setRetrievalStrategy(result.getRetrievalStrategy());

        // 各阶段命中统计
        vo.setVectorHitCount(result.getVectorDocs() == null ? 0 : result.getVectorDocs().size());
        vo.setKeywordHitCount(result.getKeywordDocs() == null ? 0 : result.getKeywordDocs().size());
        vo.setFusedHitCount(result.getFusedDocs() == null ? 0 : result.getFusedDocs().size());
        vo.setFinalHitCount(result.getDocs() == null ? 0 : result.getDocs().size());

        // 去重统计
        vo.setOverlapCount(computeOverlapCount(result.getVectorDocs(), result.getKeywordDocs()));

        // 相似度统计
        computeSimilarityStats(finalHitVOs, vo);
        return vo;
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
        double totalPrecision = 0;
        double totalMRR = 0;
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
                itemResult.setPrecision(finalResults.isEmpty() ? 0D : hitCount * 1.0D / finalResults.size());

                // MRR (1 / first hit rank)
                double mrr = 0;
                for (int i = 0; i < recalledIds.size(); i++) {
                    if (expectedIds.contains(recalledIds.get(i))) {
                        mrr = 1.0D / (i + 1);
                        break;
                    }
                }
                itemResult.setMrr(mrr);

                totalHitRate += (hitCount > 0 ? 1 : 0);
                totalRecall += itemResult.getRecall();
                totalPrecision += itemResult.getPrecision();
                totalMRR += itemResult.getMrr();
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
            vo.setMeanPrecision(totalPrecision / testCountWithExpectation);
            vo.setMeanMRR(totalMRR / testCountWithExpectation);
        }
        return vo;
    }

    @Override
    public void saveHistory(String question, String answer, Long knowledgeBaseId, Long userId,
            String sources, Long responseTime) {
        saveHistory(question, answer, knowledgeBaseId, userId, sources, responseTime, null, null, null);
    }

    @Override
    public Page<RAGHistoryVO> listRAGHistoryVOByPage(RAGHistoryQueryRequest queryRequest, HttpServletRequest request) {
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Long knowledgeBaseId = queryRequest.getKnowledgeBaseId();
        Long userId = queryRequest.getUserId();

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

    private void saveHistory(String question, String answer, Long knowledgeBaseId, Long userId,
            String sources, Long responseTime, String rewriteQuery,
            String retrievalMeta, String retrievalStrategy) {
        RAGHistory ragHistory = new RAGHistory();
        ragHistory.setQuestion(question);
        ragHistory.setAnswer(answer);
        ragHistory.setKnowledgeBaseId(knowledgeBaseId == null ? 0L : knowledgeBaseId);
        ragHistory.setUserId(userId);
        ragHistory.setSources(sources);
        ragHistory.setResponseTime(responseTime);
        ragHistory.setRewriteQuery(rewriteQuery);
        ragHistory.setRetrievalMeta(retrievalMeta);
        ragHistory.setRetrievalStrategy(retrievalStrategy);
        ragHistoryMapper.insert(ragHistory);
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
            sourceVO.setVectorSimilarity(toDoubleValue(doc.getMetadata().get("distance")));
            sourceVO.setKeywordRelevance(toDoubleValue(doc.getMetadata().get("keywordScore")));
            sourceList.add(sourceVO);
        }
        return sourceList;
    }

    private List<RetrievalHitVO> convertToHitVOs(List<Document> docs) {
        if (CollUtil.isEmpty(docs)) {
            return new ArrayList<>();
        }
        return docs.stream().map(doc -> {
            RetrievalHitVO hit = new RetrievalHitVO();
            hit.setId(doc.getId());
            hit.setDocumentId(toLongValue(doc.getMetadata().get("documentId")));
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

    private int computeOverlapCount(List<Document> vectorDocs, List<Document> keywordDocs) {
        if (CollUtil.isEmpty(vectorDocs) || CollUtil.isEmpty(keywordDocs)) {
            return 0;
        }
        Set<String> vectorKeys = new HashSet<>();
        for (Document doc : vectorDocs) {
            vectorKeys.add(buildDocKey(doc));
        }
        int count = 0;
        for (Document doc : keywordDocs) {
            if (vectorKeys.contains(buildDocKey(doc))) {
                count++;
            }
        }
        return count;
    }

    private String buildDocKey(Document doc) {
        Object documentId = doc.getMetadata().get("documentId");
        Object chunkIndex = doc.getMetadata().get("chunkIndex");
        if (documentId != null && chunkIndex != null) {
            return documentId + "_" + chunkIndex;
        }
        return doc.getId() != null ? doc.getId() : String.valueOf(doc.getText().hashCode());
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

    private Long toLongValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
