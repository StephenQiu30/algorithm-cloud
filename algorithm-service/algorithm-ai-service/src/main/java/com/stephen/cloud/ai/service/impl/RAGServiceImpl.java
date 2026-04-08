package com.stephen.cloud.ai.service.impl;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.advisor.ReReadingAdvisor;
import com.stephen.cloud.ai.config.RagGenerationProperties;
import com.stephen.cloud.ai.config.RagWebSearchProperties;
import com.stephen.cloud.ai.convert.RAGConvert;
import com.stephen.cloud.ai.knowledge.retrieval.RagDocumentHelper;
import com.stephen.cloud.ai.knowledge.retrieval.RagWebSearchFallbackDecider;
import com.stephen.cloud.ai.knowledge.retrieval.RetrievalOrchestrator;
import com.stephen.cloud.ai.knowledge.retrieval.model.RetrievalResult;
import com.stephen.cloud.ai.knowledge.retrieval.model.WebSearchFallbackDecision;
import com.stephen.cloud.ai.mapper.RAGHistoryMapper;
import com.stephen.cloud.ai.model.entity.RAGHistory;
import com.stephen.cloud.ai.service.RAGService;
import com.stephen.cloud.api.ai.model.enums.RetrievalStrategyEnum;
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
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.*;


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

    private static final String WEB_SEARCH_SYSTEM_PROMPT = """
            你是一个严谨的联网搜索问答助手。
            当前本地知识库不足以回答用户问题，请基于联网搜索到的最新资料作答。
            如果问题涉及时间敏感信息，请明确写出具体日期或时间；如果搜索后仍然无法确认，请直接说明。
            不要编造搜索结果，也不要把未经检索确认的信息当作事实给出。
            """;

    private static final String WEB_SEARCH_SOURCE_TYPE = "web_search";

    private static final String WEB_SEARCH_SOURCE_NAME = "DashScope 联网搜索";

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
    private RagWebSearchProperties ragWebSearchProperties;

    @Resource
    private RagDocumentHelper ragDocumentHelper;

    @Resource
    private ReReadingAdvisor reReadingAdvisor;

    @Resource
    private RagWebSearchFallbackDecider ragWebSearchFallbackDecider;

    @Override
    public Flux<String> askStream(String question, Long knowledgeBaseId, Long userId, Integer topK,
            String conversationId, Boolean enableWebSearchFallback) {
        return Flux.defer(() -> {
            long start = System.currentTimeMillis();
            boolean requestAllowsWebSearchFallback = !Boolean.FALSE.equals(enableWebSearchFallback);
            String resolvedConversationId = resolveConversationId(conversationId, knowledgeBaseId, userId);
            List<Message> history = loadConversationHistory(resolvedConversationId);
            RetrievalResult result = retrievalOrchestrator.retrieve(question, knowledgeBaseId, topK, history);
            WebSearchFallbackDecision fallbackDecision = ragWebSearchFallbackDecider
                    .decide(result, requestAllowsWebSearchFallback);
            String retrievalMeta = buildHistoryRetrievalMeta(result, fallbackDecision, false,
                    requestAllowsWebSearchFallback);
            List<Document> docs = result.getDocs();
            if (fallbackDecision.shouldFallback()) {
                List<SourceVO> webSearchSources = buildWebSearchSources(fallbackDecision);
                String webSearchRetrievalMeta = buildHistoryRetrievalMeta(result, fallbackDecision, true,
                        requestAllowsWebSearchFallback);
                StringBuilder answerBuilder = new StringBuilder();
                return buildWebSearchRequest(question, resolvedConversationId)
                        .stream()
                        .content()
                        .doOnNext(answerBuilder::append)
                        .doOnComplete(() -> {
                            long responseTime = System.currentTimeMillis() - start;
                            saveHistory(question, answerBuilder.toString(), knowledgeBaseId, userId,
                                    JSONUtil.toJsonStr(webSearchSources), responseTime,
                                    result.getRewriteQuery(), webSearchRetrievalMeta,
                                    RetrievalStrategyEnum.WEB_SEARCH_FALLBACK.getValue());
                        })
                        .onErrorResume(ex -> {
                            log.error("[RAG] 联网搜索兜底失败, question={}, error={}", question, ex.getMessage(), ex);
                            appendConversationMemory(resolvedConversationId, question, NO_CONTEXT_ANSWER);
                            long responseTime = System.currentTimeMillis() - start;
                            saveHistory(question, NO_CONTEXT_ANSWER, knowledgeBaseId, userId,
                                    JSONUtil.toJsonStr(webSearchSources), responseTime,
                                    result.getRewriteQuery(), webSearchRetrievalMeta,
                                    RetrievalStrategyEnum.WEB_SEARCH_FALLBACK.getValue());
                            return Flux.just(NO_CONTEXT_ANSWER);
                        });
            }
            List<SourceVO> sources = buildSources(docs);
            if (CollUtil.isEmpty(docs)) {
                appendConversationMemory(resolvedConversationId, question, NO_CONTEXT_ANSWER);
                long responseTime = System.currentTimeMillis() - start;
                saveHistory(question, NO_CONTEXT_ANSWER, knowledgeBaseId, userId,
                        JSONUtil.toJsonStr(sources), responseTime,
                        result.getRewriteQuery(), retrievalMeta, result.getRetrievalStrategy());
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
                                result.getRewriteQuery(), retrievalMeta, result.getRetrievalStrategy());
                    })
                    .onErrorResume(ex -> {
                        log.error("[RAG] 知识库问答失败, question={}, error={}", question, ex.getMessage(), ex);
                        appendConversationMemory(resolvedConversationId, question, NO_CONTEXT_ANSWER);
                        long responseTime = System.currentTimeMillis() - start;
                        saveHistory(question, NO_CONTEXT_ANSWER, knowledgeBaseId, userId,
                                JSONUtil.toJsonStr(sources), responseTime,
                                result.getRewriteQuery(), retrievalMeta, result.getRetrievalStrategy());
                        return Flux.just(NO_CONTEXT_ANSWER);
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
        return ragDocumentHelper.toSourceVOs(docs);
    }

    private List<RetrievalHitVO> convertToHitVOs(List<Document> docs) {
        return ragDocumentHelper.toRetrievalHitVOs(docs);
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

    private ChatClient.ChatClientRequestSpec buildWebSearchRequest(String question, String conversationId) {
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        return chatClient.prompt()
                .options(buildWebSearchGenerationOptions())
                .advisors(memoryAdvisor, reReadingAdvisor)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .system(WEB_SEARCH_SYSTEM_PROMPT)
                .user(question);
    }

    private DashScopeChatOptions buildGenerationOptions() {
        DashScopeChatOptions.DashScopeChatOptionsBuilder builder = DashScopeChatOptions.builder();
        if (ragGenerationProperties.getTemperature() != null) {
            builder.temperature(ragGenerationProperties.getTemperature());
        }
        return builder.build();
    }

    private DashScopeChatOptions buildWebSearchGenerationOptions() {
        DashScopeChatOptions options = buildGenerationOptions();
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(options);
        if (!beanWrapper.isWritableProperty("enableSearch")) {
            throw new IllegalStateException("当前 DashScopeChatOptions 版本不支持 enableSearch 配置");
        }
        beanWrapper.setPropertyValue("enableSearch", true);
        return options;
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
                    .append(", documentId=").append(toStringValue(doc.getMetadata().get(DOCUMENT_ID)))
                    .append(", documentName=").append(toStringValue(doc.getMetadata().get(DOCUMENT_NAME)))
                    .append(", chunkIndex=").append(toStringValue(doc.getMetadata().get(CHUNK_INDEX)));
            String sectionPath = toStringValue(doc.getMetadata().get(SECTION_PATH));
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

    private String buildHistoryRetrievalMeta(RetrievalResult result, WebSearchFallbackDecision fallbackDecision,
            boolean webSearchTriggered, boolean requestAllowsWebSearchFallback) {
        JSONObject meta = StringUtils.isNotBlank(result.getRetrievalMeta())
                ? JSONUtil.parseObj(result.getRetrievalMeta())
                : JSONUtil.createObj();
        meta.set("knowledgeRetrievalStrategy", result.getRetrievalStrategy());
        meta.set("webSearchConfiguredEnabled", ragWebSearchProperties.isEnabled());
        meta.set("webSearchRequestEnabled", requestAllowsWebSearchFallback);
        meta.set("webSearchTriggered", webSearchTriggered);
        meta.set("webSearchDecisionReason", fallbackDecision.reason());
        meta.set("webSearchKnowledgeHitCount", fallbackDecision.knowledgeHitCount());
        if (fallbackDecision.topVectorSimilarity() != null) {
            meta.set("webSearchTopVectorSimilarity", fallbackDecision.topVectorSimilarity());
        }
        if (fallbackDecision.averageVectorSimilarity() != null) {
            meta.set("webSearchAverageVectorSimilarity", fallbackDecision.averageVectorSimilarity());
        }
        return meta.toString();
    }

    private List<SourceVO> buildWebSearchSources(WebSearchFallbackDecision fallbackDecision) {
        SourceVO source = new SourceVO();
        source.setDocumentName(WEB_SEARCH_SOURCE_NAME);
        source.setSourceType(WEB_SEARCH_SOURCE_TYPE);
        source.setSectionTitle("联网搜索兜底");
        source.setBizTag("web-search-fallback");
        source.setMatchReason(fallbackDecision.reason());
        source.setContent("知识库召回不足，本次回答已切换为 DashScope 联网搜索。");
        source.setVectorSimilarity(fallbackDecision.topVectorSimilarity());
        return List.of(source);
    }

}
