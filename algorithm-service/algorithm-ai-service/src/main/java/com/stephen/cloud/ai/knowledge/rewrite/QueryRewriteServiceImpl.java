package com.stephen.cloud.ai.knowledge.rewrite;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.stephen.cloud.ai.config.RagRetrievalProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 查询改写服务
 * <p>
 * 支持两种模式：
 * <ul>
 *   <li>规则改写（默认）：基于正则和硬编码同义词进行改写</li>
 *   <li>LLM 改写（可配置开启）：使用大模型进行语义改写、关键词抽取和多查询拆分</li>
 * </ul>
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Service
public class QueryRewriteServiceImpl implements QueryRewriteService {

    private static final Pattern VERSION_PATTERN = Pattern.compile("\\b(?:v)?\\d+(?:\\.\\d+){1,2}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("\\b[A-Z]{2,10}-\\d{2,8}\\b");

    @Resource
    private RagRetrievalProperties ragRetrievalProperties;

    @Resource
    private ChatClient chatClient;

    @Override
    public RewriteResult rewrite(String question) {
        if (ragRetrievalProperties.isLlmRewriteEnabled()) {
            try {
                return llmRewrite(question);
            } catch (Exception e) {
                log.warn("[QueryRewrite] LLM 改写失败, 降级为规则改写, error={}", e.getMessage());
                return ruleRewrite(question);
            }
        }
        return ruleRewrite(question);
    }

    /**
     * LLM 增强改写：语义优化 + 关键词抽取 + 多查询拆分
     */
    private RewriteResult llmRewrite(String question) {
        String prompt = buildLlmRewritePrompt(question);
        String response = chatClient.prompt().user(prompt).call().content();
        log.info("[QueryRewrite] LLM 改写完成, original={}, response={}", question, response);
        return parseLlmResponse(response, question);
    }

    /**
     * 构造 LLM 改写 Prompt
     */
    private String buildLlmRewritePrompt(String question) {
        return """
                你是一个专业的搜索查询改写助手。请对用户的查询进行以下处理，并以 JSON 格式返回结果：
                
                1. semanticQuery: 将用户查询改写为更适合向量语义搜索的形式，保持语义不变但使表达更清晰完整
                2. keywordQuery: 提取查询中的核心关键词，用空格分隔，适当添加同义词
                3. subQueries: 如果原始查询包含多个子问题或多个维度，拆分为多个独立子查询（数组形式）；如果查询简单直接，返回空数组
                
                要求：
                - 只返回 JSON，不要返回其他内容
                - subQueries 最多拆分 3 个子查询
                
                用户查询：""" + question + """
                
                返回格式：
                {"semanticQuery": "...", "keywordQuery": "...", "subQueries": ["...", "..."]}
                """;
    }

    /**
     * 解析 LLM 返回的 JSON
     */
    private RewriteResult parseLlmResponse(String response, String originalQuestion) {
        RewriteResult result = new RewriteResult();
        try {
            // 提取 JSON 部分（LLM 可能在 JSON 外包裹额外文本）
            String jsonStr = extractJson(response);
            if (StringUtils.isNotBlank(jsonStr)) {
                JSONObject json = JSONUtil.parseObj(jsonStr);
                result.setSemanticQuery(json.getStr("semanticQuery", originalQuestion));
                result.setKeywordQuery(json.getStr("keywordQuery", originalQuestion));
                List<String> subQueries = json.getBeanList("subQueries", String.class);
                result.setSubQueries(CollUtil.isEmpty(subQueries) ? List.of() : subQueries);
            } else {
                result.setSemanticQuery(originalQuestion);
                result.setKeywordQuery(originalQuestion);
                result.setSubQueries(List.of());
            }
        } catch (Exception e) {
            log.warn("[QueryRewrite] 解析 LLM 响应失败, 使用原始查询, error={}", e.getMessage());
            result.setSemanticQuery(originalQuestion);
            result.setKeywordQuery(originalQuestion);
            result.setSubQueries(List.of());
        }
        // 补充规则层的 mustTerms 和 metadataFilters
        result.setMustTerms(extractMustTerms(originalQuestion));
        result.setMetadataFilters(extractMetadataFilters(originalQuestion));
        return result;
    }

    /**
     * 从文本中提取第一个 JSON 对象
     */
    private String extractJson(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
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

    private String expandSynonyms(String query) {
        if (StringUtils.isBlank(query)) {
            return "";
        }
        String output = query;
        output = output.replace("异常", "异常 错误");
        output = output.replace("报错", "报错 错误");
        output = output.replace("失败", "失败 错误");
        output = output.replace("优化", "优化 提升 性能");
        output = output.replace("配置", "配置 参数 设置");
        return output;
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
            filters.put("version", versionMatcher.group());
        }
        if (query.contains("接口") || query.contains("api") || query.contains("调用")) {
            filters.put("bizTag", "api");
        } else if (query.contains("部署") || query.contains("发布") || query.contains("配置")) {
            filters.put("bizTag", "deploy");
        } else if (query.contains("性能") || query.contains("压力") || query.contains("耗时")) {
            filters.put("bizTag", "perf");
        }
        return filters;
    }
}
