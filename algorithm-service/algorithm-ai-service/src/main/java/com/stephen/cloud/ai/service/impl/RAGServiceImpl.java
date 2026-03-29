package com.stephen.cloud.ai.service.impl;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.advisor.ReReadingAdvisor;
import com.stephen.cloud.ai.config.RagGenerationProperties;
import com.stephen.cloud.ai.convert.RAGConvert;
import com.stephen.cloud.ai.knowledge.retrieval.RagDocumentHelper;
import com.stephen.cloud.ai.knowledge.retrieval.RetrievalOrchestrator;
import com.stephen.cloud.ai.knowledge.retrieval.model.RetrievalResult;
import com.stephen.cloud.ai.mapper.RAGHistoryMapper;
import com.stephen.cloud.ai.model.entity.RAGHistory;
import com.stephen.cloud.ai.service.RAGService;
import com.stephen.cloud.api.ai.model.dto.rag.BatchRecallRequest;
import com.stephen.cloud.api.ai.model.dto.rag.RAGHistoryQueryRequest;
import com.stephen.cloud.api.ai.model.dto.rag.RecallAnalysisRequest;
import com.stephen.cloud.api.ai.model.dto.rag.RecallTestItem;
import com.stephen.cloud.api.ai.model.vo.BatchRecallVO;
import com.stephen.cloud.api.ai.model.vo.RAGHistoryVO;
import com.stephen.cloud.api.ai.model.vo.RecallAnalysisVO;
import com.stephen.cloud.api.ai.model.vo.RecallItemResultVO;
import com.stephen.cloud.api.ai.model.vo.RetrievalHitVO;
import com.stephen.cloud.api.ai.model.vo.SourceVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
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
 * 当前仅保留流式问答入口，检索编排逻辑已提取到 {@link RetrievalOrchestrator}，本类专注于上层业务。
 * </p>
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class RAGServiceImpl implements RAGService {

    private static final String RAG_SYSTEM_PROMPT = """
            你是一个严谨的知识库问答助手。
            请仅依据检索到的知识片段回答用户问题；不得编造片段中不存在的事实或数据。
            如果上下文不足，请明确说明“当前知识库中没有足够信息支持该结论”。
            引用依据时请使用片段序号或片段头中的 chunkId；回答尽量简洁、准确。
            """;

    private static final PromptTemplate RAG_CONTEXTUAL_QUERY_AUGMENT_TEMPLATE = new PromptTemplate("""
            以下是仅用于本题作答的检索片段：

            ---------------------
            {context}
            ---------------------

            用户问题（若含重复表述以辅助阅读，请抓住实质问题）：
            {query}

            请仅依据上述片段作答；引用时标明片段序号或 chunkId；信息不足时直接说明，不要臆测。
            """);

    private static final String NO_CONTEXT_ANSWER = "当前知识库没有检索到足够相关的内容，暂时无法给出可靠回答。";

    @Resource
    private ChatClient chatClient;

    @Resource
    private ChatMemory chatMemory;

    @Resource
    private RAGHistoryMapper ragHistoryMapper;

    @Resource
    private RetrievalOrchestrator retrievalOrchestrator;

    @Resource
    private RagGenerationProperties ragGenerationProperties;

    @Resource
    private RagDocumentHelper ragDocumentHelper;

    @Resource
    private ReReadingAdvisor reReadingAdvisor;

    @Override
    public Flux<String> askStream(String question, Long knowledgeBaseId, Long userId, Integer topK,
            String conversationId) {
        return Flux.defer(() -> {
            long start = System.currentTimeMillis();
            String resolvedConversationId = resolveConversationId(conversationId, knowledgeBaseId, userId);
            List<Message> history = loadConversationHistory(resolvedConversationId);
            RetrievalResult result = retrievalOrchestrator.retrieve(question, knowledgeBaseId, topK, history);
            List<Document> docs = result.getDocs();
            List<SourceVO> sources = buildSources(docs);
            if (CollUtil.isEmpty(docs)) {
                appendConversationMemory(resolvedConversationId, question, NO_CONTEXT_ANSWER);
                long responseTime = System.currentTimeMillis() - start;
                saveHistory(question, NO_CONTEXT_ANSWER, knowledgeBaseId, userId,
                        JSONUtil.toJsonStr(sources), responseTime,
                        result.getRewriteQuery(), result.getRetrievalMeta(), result.getRetrievalStrategy());
                return Flux.just(NO_CONTEXT_ANSWER);
            }
            StringBuilder answerBuilder = new StringBuilder();
            return buildRagRequest(question, resolvedConversationId, docs)
                    .stream()
                    .content()
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
            if (documentId != null) {
                sourceVO.setDocumentId(Long.valueOf(String.valueOf(documentId)));
            }
            sourceVO.setDocumentName(documentName == null ? null : String.valueOf(documentName));
            if (chunkIndex != null) {
                sourceVO.setChunkIndex(Integer.valueOf(String.valueOf(chunkIndex)));
            }
            sourceVO.setChunkId(ragDocumentHelper.resolveChunkId(doc));
            sourceVO.setSourceType(toStringValue(doc.getMetadata().get("sourceType")));
            sourceVO.setVersion(toStringValue(doc.getMetadata().get("version")));
            sourceVO.setBizTag(toStringValue(doc.getMetadata().get("bizTag")));
            sourceVO.setSectionTitle(toStringValue(doc.getMetadata().get("sectionTitle")));
            sourceVO.setSectionPath(toStringValue(doc.getMetadata().get("sectionPath")));
            sourceVO.setMatchReason(toStringValue(doc.getMetadata().get("matchReason")));
            sourceVO.setContent(doc.getText());
            sourceVO.setScore(defaultScore(doc));
            sourceVO.setVectorSimilarity(ragDocumentHelper.resolveVectorScore(doc));
            sourceVO.setKeywordRelevance(ragDocumentHelper.resolveKeywordScore(doc));
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
            String chunkId = ragDocumentHelper.resolveChunkId(doc);
            hit.setId(chunkId);
            hit.setChunkId(chunkId);
            hit.setDocumentId(toLongValue(doc.getMetadata().get("documentId")));
            hit.setContent(doc.getText());
            hit.setDocumentName(toStringValue(doc.getMetadata().get("documentName")));
            hit.setChunkIndex(toIntegerValue(doc.getMetadata().get("chunkIndex")));
            hit.setSectionTitle(toStringValue(doc.getMetadata().get("sectionTitle")));
            hit.setSectionPath(toStringValue(doc.getMetadata().get("sectionPath")));
            hit.setVectorScore(ragDocumentHelper.resolveVectorScore(doc));
            hit.setKeywordScore(ragDocumentHelper.resolveKeywordScore(doc));
            hit.setFusionScore(ragDocumentHelper.resolveFusionScore(doc));
            hit.setScore(defaultScore(doc));
            hit.setSimilarityScore(ragDocumentHelper.resolveVectorScore(doc));
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
        return ragDocumentHelper.buildDocKey(doc);
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

    private ChatClient.ChatClientRequestSpec buildRagRequest(String question, String conversationId, List<Document> docs) {
        RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(query -> docs)
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .promptTemplate(RAG_CONTEXTUAL_QUERY_AUGMENT_TEMPLATE)
                        .allowEmptyContext(true)
                        .documentFormatter(this::formatDocumentsForPrompt)
                        .build())
                .order(1)
                .build();

        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return chatClient.prompt()
                .options(buildGenerationOptions())
                .advisors(memoryAdvisor, reReadingAdvisor, ragAdvisor)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .system(RAG_SYSTEM_PROMPT)
                .user(question);
    }

    private DashScopeChatOptions buildGenerationOptions() {
        DashScopeChatOptions.DashScopeChatOptionsBuilder builder = DashScopeChatOptions.builder();
        if (ragGenerationProperties.getTemperature() != null) {
            builder.temperature(ragGenerationProperties.getTemperature());
        }
        return builder.build();
    }

    private String formatDocumentsForPrompt(List<Document> docs) {
        if (CollUtil.isEmpty(docs)) {
            return "";
        }
        int budget = Math.max(256, ragGenerationProperties.getMaxContextLength());
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            String chunkId = ragDocumentHelper.resolveChunkId(doc);
            StringBuilder headerBuilder = new StringBuilder("片段")
                    .append(i + 1)
                    .append(" [chunkId=").append(chunkId)
                    .append(", documentId=").append(toStringValue(doc.getMetadata().get("documentId")))
                    .append(", documentName=").append(toStringValue(doc.getMetadata().get("documentName")))
                    .append(", chunkIndex=").append(toStringValue(doc.getMetadata().get("chunkIndex")));
            String sectionPath = toStringValue(doc.getMetadata().get("sectionPath"));
            if (StringUtils.isNotBlank(sectionPath)) {
                headerBuilder.append(", sectionPath=").append(sectionPath);
            }
            headerBuilder.append("]\n");
            String header = headerBuilder.toString();
            String content = StringUtils.defaultString(doc.getText());
            int remaining = budget - builder.length() - header.length() - 2;
            if (remaining <= 0) {
                break;
            }
            if (content.length() > remaining) {
                content = content.substring(0, remaining);
            }
            builder.append(header).append(content).append("\n\n");
            if (builder.length() >= budget) {
                break;
            }
        }
        return builder.toString();
    }

    private List<Message> loadConversationHistory(String conversationId) {
        if (StringUtils.isBlank(conversationId)) {
            return List.of();
        }
        try {
            List<Message> messages = chatMemory.get(conversationId);
            if (CollUtil.isEmpty(messages)) {
                return List.of();
            }
            List<Message> history = new ArrayList<>();
            for (Message message : messages) {
                if (message == null) {
                    continue;
                }
                MessageType messageType = message.getMessageType();
                if (MessageType.USER.equals(messageType) || MessageType.ASSISTANT.equals(messageType)) {
                    history.add(message);
                }
            }
            return history;
        } catch (Exception e) {
            log.warn("[RAG] 加载会话历史失败, conversationId={}, error={}", conversationId, e.getMessage());
            return List.of();
        }
    }

    private void appendConversationMemory(String conversationId, String question, String answer) {
        if (StringUtils.isBlank(conversationId)) {
            return;
        }
        chatMemory.add(conversationId, List.of(
                new UserMessage(question),
                new AssistantMessage(answer)
        ));
    }

    private String resolveConversationId(String conversationId, Long knowledgeBaseId, Long userId) {
        if (StringUtils.isNotBlank(conversationId)) {
            return conversationId.trim();
        }
        String userPart = userId == null ? "anonymous" : String.valueOf(userId);
        String kbPart = knowledgeBaseId == null ? "0" : String.valueOf(knowledgeBaseId);
        return "rag:" + userPart + ":" + kbPart;
    }

    private Double defaultScore(Document doc) {
        Double fusionScore = ragDocumentHelper.resolveFusionScore(doc);
        if (fusionScore != null) {
            return fusionScore;
        }
        Double vectorScore = ragDocumentHelper.resolveVectorScore(doc);
        if (vectorScore != null) {
            return vectorScore;
        }
        return ragDocumentHelper.resolveKeywordScore(doc);
    }
}
