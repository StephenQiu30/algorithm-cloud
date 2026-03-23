package com.stephen.cloud.ai.knowledge.processor;

import com.stephen.cloud.ai.config.KnowledgeProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文本分片工具 (Transform Phase)
 * <p>
 * 遵循 Spring AI {@link org.springframework.ai.transformer.DocumentTransformer} 设计理念。
 * 核心逻辑：先按语义段落（空行）预拆分，在保证语义完整的前提下，利用 {@link TokenTextSplitter}
 * 进行精确的 Token 计数拆分。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class TextChunker {

    private final TokenTextSplitter splitter;
    private final KnowledgeProperties knowledgeProperties;

    public TextChunker(KnowledgeProperties knowledgeProperties) {
        this.knowledgeProperties = knowledgeProperties;
        // 初始化 Token 拆分器
        this.splitter = new TokenTextSplitter(
                knowledgeProperties.getChunkSize(),
                knowledgeProperties.getChunkOverlap(),
                5,    // 最小字符数
                10000, // 最大迭代次数
                true  // 保留分隔符
        );
    }

    /**
     * 将解析后的文档列表转换为语义分片列表
     *
     * @param cleanedDocuments 已提取并清洗的原始文档列表
     * @return 最终的语义分片列表（带完整元数据）
     */
    public List<Document> splitToChunkDocuments(List<Document> cleanedDocuments) {
        if (cleanedDocuments == null || cleanedDocuments.isEmpty()) {
            return List.of();
        }

        List<Document> finalChunks = new ArrayList<>();
        int mergeBudget = knowledgeProperties.getChunkParagraphMergeCharBudget();

        for (Document originalDoc : cleanedDocuments) {
            String text = originalDoc.getText();
            if (StringUtils.isBlank(text)) {
                continue;
            }

            // 1. 基于空行进行段落级的初步拆分与合并（保持语义原子性）
            List<String> paragraphBlocks = mergeParagraphBlocks(text.trim(), mergeBudget);

            // 2. 将段落包装为临时 Document 对象以便调用 Spring AI Splitter
            List<Document> blockDocs = new ArrayList<>();
            for (String block : paragraphBlocks) {
                if (StringUtils.isNotBlank(block)) {
                    // 继承原始文档的所有元数据
                    Map<String, Object> metadata = originalDoc.getMetadata() != null
                            ? new HashMap<>(originalDoc.getMetadata())
                            : new HashMap<>();
                    blockDocs.add(new Document(block, metadata));
                }
            }

            if (blockDocs.isEmpty()) {
                continue;
            }

            // 3. 使用 Spring AI 的 TokenTextSplitter 进行最终的规格化拆分
            // apply 方法会自动处理元数据的继承
            List<Document> splitted = splitter.apply(blockDocs);
            for (Document chunk : splitted) {
                String chunkText = chunk.getText() != null ? chunk.getText().trim() : "";
                if (StringUtils.isNotBlank(chunkText)) {
                    finalChunks.add(chunk);
                }
            }
        }

        log.debug("分片完成: 原始文档数={}, 产生分片数={}", cleanedDocuments.size(), finalChunks.size());
        return finalChunks;
    }

    /**
     * 按段落（空行）预拆分，并在字符预算（budget）内合并短段落。
     * 目的：防止过度碎片化，尽可能保持上下文的完整性。
     */
    private List<String> mergeParagraphBlocks(String content, int budget) {
        // 使用正则匹配 2 个或更多换行符作为段落分隔符
        String[] parts = content.split("\\r?\\n\\s*\\n");
        List<String> merged = new ArrayList<>();
        StringBuilder currentBlock = new StringBuilder();

        for (String part : parts) {
            String trimmedPart = part.trim();
            if (trimmedPart.isEmpty()) {
                continue;
            }

            // 如果当前块加上新段落超过预算，则结算当前块
            if (currentBlock.length() > 0 && currentBlock.length() + trimmedPart.length() + 2 > budget) {
                merged.add(currentBlock.toString());
                currentBlock = new StringBuilder();
            }

            if (currentBlock.length() > 0) {
                currentBlock.append("\n\n");
            }
            currentBlock.append(trimmedPart);
        }

        if (currentBlock.length() > 0) {
            merged.add(currentBlock.toString());
        }

        return merged.isEmpty() ? List.of(content) : merged;
    }
}
