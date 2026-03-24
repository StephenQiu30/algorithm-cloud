package com.stephen.cloud.ai.knowledge.rewrite;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QueryRewriteServiceImpl implements QueryRewriteService {

    private static final Pattern VERSION_PATTERN = Pattern.compile("\\b(?:v)?\\d+(?:\\.\\d+){1,2}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("\\b[A-Z]{2,10}-\\d{2,8}\\b");

    @Override
    public RewriteResult rewrite(String question) {
        String normalized = normalize(question);
        RewriteResult result = new RewriteResult();
        result.setSemanticQuery(normalized);
        result.setKeywordQuery(expandSynonyms(normalized));
        result.setMustTerms(extractMustTerms(normalized));
        result.setMetadataFilters(extractMetadataFilters(normalized));
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
        if (query.contains("接口") || query.contains("api")) {
            filters.put("bizTag", "api");
        } else if (query.contains("部署") || query.contains("发布")) {
            filters.put("bizTag", "deploy");
        }
        return filters;
    }
}
