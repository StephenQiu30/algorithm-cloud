package com.stephen.cloud.ai.knowledge.util;

import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.knowledge.etl.ContentTextCleaner;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文本分片工具：用于教学类文档（含排序算法说明）入库前的语义切分。
 * <p>
 * 符合 Spring AI {@link org.springframework.ai.document.DocumentTransformer} 的逻辑，
 * 先 {@link ContentTextCleaner} 清洗，再按空行拆段并在
 * {@link KnowledgeProperties#getChunkParagraphMergeCharBudget()} 预算内合并短段，最后
 * {@link TokenTextSplitter} 按 {@link KnowledgeProperties#getChunkSize()} /
 * {@link KnowledgeProperties#getChunkOverlap()} 切分，兼顾段落边界与检索窗口。
 * </p>
 *
 * @author StephenQiu30
 */
@Component
public class TextChunker {

    private final TokenTextSplitter splitter;
    private final KnowledgeProperties knowledgeProperties;
    private final ContentTextCleaner contentTextCleaner;

    public TextChunker(KnowledgeProperties knowledgeProperties, ContentTextCleaner contentTextCleaner) {
        this.knowledgeProperties = knowledgeProperties;
        this.contentTextCleaner = contentTextCleaner;
        // 显式配置 TokenTextSplitter，确保参数生效
        this.splitter = new TokenTextSplitter(
                knowledgeProperties.getChunkSize(),
                knowledgeProperties.getChunkOverlap(),
                5, // 固定长度（保持默认）
                10000, // 最大分段数
                true); // 保持格式
    }

    /**
     * 将全文切分为多条非空分片（清洗 → 段落合并 → token 分片）
     *
     * @param text 抽取后的原始文本
     * @return 分片列表
     */
    public List<String> splitWithOverlap(String text) {
        String cleaned = contentTextCleaner.clean(text);
        if (StringUtils.isBlank(cleaned)) {
            return List.of();
        }
        // 1. 段落合并，避免过度切分
        List<String> segments = mergeParagraphBlocks(cleaned, knowledgeProperties.getChunkParagraphMergeCharBudget());
        
        // 2. 将段落转为 Document 后使用 TokenTextSplitter 进行最终分片
        List<Document> documents = segments.stream()
                .map(Document::new)
                .collect(Collectors.toList());
        
        List<Document> chunks = splitter.apply(documents);
        
        return chunks.stream()
                .map(Document::getText)
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    /**
     * 按空行分段，并在预算内合并过短段落，减少跨语义硬切分。
     * 对于算法题或代码块，合并段落能保持上下文完整性。
     */
    private List<String> mergeParagraphBlocks(String cleaned, int budget) {
        // 适配多种换行符
        String[] parts = cleaned.split("\\r?\\n\\s*\\n");
        List<String> merged = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) {
                continue;
            }
            // 超过预算则存入并开启新段落
            if (cur.length() > 0 && cur.length() + t.length() + 2 > budget) {
                merged.add(cur.toString());
                cur = new StringBuilder();
            }
            if (cur.length() > 0) {
                cur.append("\n\n");
            }
            cur.append(t);
        }
        if (cur.length() > 0) {
            merged.add(cur.toString());
        }
        if (merged.isEmpty()) {
            merged.add(cleaned);
        }
        return merged;
    }
}
