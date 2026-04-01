package com.stephen.cloud.ai.knowledge.rewrite;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import cn.hutool.core.collection.CollUtil;
import com.stephen.cloud.ai.config.RagRetrievalProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.BIZ_TAG;
import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.VERSION;

/**
 * 查询改写服务
 * <p>
 * 支持两种模式：
 * <ul>
 *   <li>规则改写（默认）：基于正则和可配置同义词表进行改写</li>
 *   <li>LLM 改写（可配置开启）：使用大模型进行语义改写、关键词抽取和多查询拆分</li>
 * </ul>
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Service
public class QueryRewriteServiceImpl implements QueryRewriteService {

    private static final int DEFAULT_MULTI_QUERY_COUNT = 3;

    private static final Pattern VERSION_PATTERN = Pattern.compile("\\b(?:v)?\\d+(?:\\.\\d+){1,2}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("\\b[A-Z]{2,10}-\\d{2,8}\\b");

    @Resource
    private RagRetrievalProperties ragRetrievalProperties;

    @Resource
    private ChatClient.Builder chatClientBuilder;

    @Override
    public RewriteResult rewrite(String question, List<Message> history) {
        String normalized = normalize(question);
        if (!ragRetrievalProperties.isLlmRewriteEnabled()) {
            return ruleRewrite(normalized);
        }
        try {
            return llmRewrite(normalized, history);
        } catch (Exception e) {
            log.warn("[QueryRewrite] Spring AI 官方改写失败, 降级为规则改写, error={}", e.getMessage());
            return ruleRewrite(normalized);
        }
    }

    /**
     * 基于 Spring AI 官方 QueryTransformer / MultiQueryExpander 的改写主链。
     */
    private RewriteResult llmRewrite(String question, List<Message> history) {
        ChatClient.Builder lowTemperatureBuilder = buildLowTemperatureChatClientBuilder();
        String compressedQuery = compressQuestion(question, history, lowTemperatureBuilder);
        String semanticQuery = rewriteSemanticQuery(compressedQuery, lowTemperatureBuilder);
        List<String> subQueries = expandQueries(semanticQuery, lowTemperatureBuilder);

        RewriteResult result = ruleRewrite(question);
        result.setSemanticQuery(StringUtils.defaultIfBlank(semanticQuery, question));
        result.setSubQueries(subQueries);
        log.info("[QueryRewrite] Spring AI 改写完成, original={}, compressed={}, semantic={}, subQueries={}",
                question, compressedQuery, result.getSemanticQuery(), subQueries);
        return result;
    }

    private ChatClient.Builder buildLowTemperatureChatClientBuilder() {
        return chatClientBuilder.clone()
                .defaultOptions(DashScopeChatOptions.builder().temperature(0.0D).build());
    }

    private String compressQuestion(String question, List<Message> history, ChatClient.Builder lowTemperatureBuilder) {
        List<Message> retrievalHistory = sanitizeHistory(history);
        if (CollUtil.isEmpty(retrievalHistory)) {
            return question;
        }
        Query query = Query.builder()
                .text(question)
                .history(retrievalHistory)
                .build();
        Query compressed = CompressionQueryTransformer.builder()
                .chatClientBuilder(lowTemperatureBuilder.clone())
                .build()
                .transform(query);
        if (compressed == null || StringUtils.isBlank(compressed.text())) {
            return question;
        }
        return compressed.text().trim();
    }

    private String rewriteSemanticQuery(String question, ChatClient.Builder lowTemperatureBuilder) {
        Query rewritten = RewriteQueryTransformer.builder()
                .chatClientBuilder(lowTemperatureBuilder.clone())
                .targetSearchSystem("enterprise knowledge base vector store")
                .build()
                .transform(new Query(question));
        if (rewritten == null || StringUtils.isBlank(rewritten.text())) {
            return question;
        }
        return rewritten.text().trim();
    }

    private List<String> expandQueries(String semanticQuery, ChatClient.Builder lowTemperatureBuilder) {
        if (!ragRetrievalProperties.isMultiQueryEnabled()) {
            return List.of();
        }
        List<Query> expandedQueries = MultiQueryExpander.builder()
                .chatClientBuilder(lowTemperatureBuilder.clone())
                .includeOriginal(Boolean.FALSE)
                .numberOfQueries(DEFAULT_MULTI_QUERY_COUNT)
                .build()
                .expand(new Query(semanticQuery));
        if (CollUtil.isEmpty(expandedQueries)) {
            return List.of();
        }
        List<String> subQueries = new ArrayList<>();
        for (Query expandedQuery : expandedQueries) {
            if (expandedQuery == null || StringUtils.isBlank(expandedQuery.text())) {
                continue;
            }
            String candidate = expandedQuery.text().trim();
            if (StringUtils.equalsIgnoreCase(candidate, semanticQuery) || subQueries.contains(candidate)) {
                continue;
            }
            subQueries.add(candidate);
        }
        return subQueries;
    }

    private List<Message> sanitizeHistory(List<Message> history) {
        if (CollUtil.isEmpty(history)) {
            return List.of();
        }
        List<Message> sanitized = new ArrayList<>();
        for (Message message : history) {
            if (message == null) {
                continue;
            }
            MessageType messageType = message.getMessageType();
            if (MessageType.USER.equals(messageType) || MessageType.ASSISTANT.equals(messageType)) {
                sanitized.add(message);
            }
        }
        return sanitized;
    }

    // ==================== 规则改写（Fallback）====================

    private RewriteResult ruleRewrite(String question) {
        String normalized = normalize(question);
        RewriteResult result = new RewriteResult();
        result.setSemanticQuery(normalized);
        result.setKeywordQuery(expandSynonyms(normalized));
        result.setMustTerms(extractMustTerms(normalized));
        result.setMetadataFilters(extractMetadataFilters(normalized));
        result.setSubQueries(List.of());
        return result;
    }

    private String normalize(String question) {
        if (StringUtils.isBlank(question)) {
            return "";
        }
        return question.replaceAll("\\s+", " ").trim();
    }

    /**
     * 同义词扩展（从配置读取同义词表，支持 Nacos 动态更新）
     */
    private String expandSynonyms(String query) {
        if (StringUtils.isBlank(query)) {
            return "";
        }
        Map<String, List<String>> synonymMap = ragRetrievalProperties.getSynonymMap();
        if (synonymMap == null || synonymMap.isEmpty()) {
            return query;
        }
        // 收集所有需要追加的同义词（去重）
        Set<String> expansions = new HashSet<>();
        String normalizedQuery = query.toLowerCase();
        for (Map.Entry<String, List<String>> entry : synonymMap.entrySet()) {
            if (normalizedQuery.contains(entry.getKey().toLowerCase())) {
                for (String synonym : entry.getValue()) {
                    if (!normalizedQuery.contains(synonym.toLowerCase())) {
                        expansions.add(synonym);
                    }
                }
            }
        }
        if (expansions.isEmpty()) {
            return query;
        }
        return query + " " + String.join(" ", expansions);
    }

    private List<String> extractMustTerms(String query) {
        List<String> terms = new ArrayList<>();
        if (StringUtils.isBlank(query)) {
            return terms;
        }
        Matcher versionMatcher = VERSION_PATTERN.matcher(query);
        while (versionMatcher.find()) {
            terms.add(versionMatcher.group());
        }
        Matcher errorMatcher = ERROR_CODE_PATTERN.matcher(query);
        while (errorMatcher.find()) {
            terms.add(errorMatcher.group());
        }
        return terms;
    }

    private Map<String, String> extractMetadataFilters(String query) {
        Map<String, String> filters = new LinkedHashMap<>();
        if (StringUtils.isBlank(query)) {
            return filters;
        }
        Matcher versionMatcher = VERSION_PATTERN.matcher(query);
        if (versionMatcher.find()) {
            filters.put(VERSION, versionMatcher.group());
        }
        if (query.contains("接口") || query.contains("api") || query.contains("调用")) {
            filters.put(BIZ_TAG, "api");
        } else if (query.contains("部署") || query.contains("发布") || query.contains("配置")) {
            filters.put(BIZ_TAG, "deploy");
        } else if (query.contains("性能") || query.contains("压力") || query.contains("耗时")) {
            filters.put(BIZ_TAG, "perf");
        }
        return filters;
    }
}
