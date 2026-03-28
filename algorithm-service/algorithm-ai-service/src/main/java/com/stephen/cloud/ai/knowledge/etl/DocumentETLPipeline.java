package com.stephen.cloud.ai.knowledge.etl;

import com.stephen.cloud.ai.knowledge.retrieval.RagDocumentHelper;
import com.stephen.cloud.ai.knowledge.reader.DocumentReaderFactory;
import com.stephen.cloud.ai.mapper.DocumentChunkMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.config.DocumentProcessingProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文档 ETL 管道
 * <p>
 * 支持三种分片策略：
 * <ul>
 *   <li>smart：基于 Markdown 标题 + 段落边界的智能分片（参考阿里云百炼平台）</li>
 *   <li>fixed_length：按固定 Token 长度切分</li>
 *   <li>by_title：按标题切分（复用 SmartTextSplitter 的标题分段逻辑）</li>
 * </ul>
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class DocumentETLPipeline {

    @Resource
    private DocumentReaderFactory documentReaderFactory;

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Resource
    private VectorStore vectorStore;

    @Resource
    private ResourceLoader resourceLoader;

    @Resource
    private DocumentChunkMapper documentChunkMapper;

    @Resource
    private DocumentProcessingProperties documentProcessingProperties;

    @Resource
    private RagDocumentHelper ragDocumentHelper;

    public int process(String filePath, String fileExtension, Map<String, Object> metadata) {
        long start = System.currentTimeMillis();
        Map<String, Object> baseMetadata = sanitizeMetadata(metadata);
        String location = resolveLocation(filePath);
        org.springframework.core.io.Resource springResource = resourceLoader.getResource(location);
        DocumentReader reader = documentReaderFactory.getReader(fileExtension, springResource);
        List<Document> documents = reader.get();

        // 注入元数据
        for (Document document : documents) {
            document.getMetadata().putAll(baseMetadata);
        }

        // 根据策略选择分片器
        String strategy = documentProcessingProperties.getChunkStrategy();
        List<Document> chunks = assignStableChunkIds(splitByStrategy(documents, strategy), baseMetadata);
        log.info("[ETL] 分片完成, strategy={}, chunkCount={}, fileExtension={}",
                strategy, chunks.size(), fileExtension);

        Long documentId = parseLong(baseMetadata.get("documentId"));
        Long knowledgeBaseId = parseLong(baseMetadata.get("knowledgeBaseId"));
        try {
            if (!chunks.isEmpty()) {
                vectorStore.add(chunks);
            }
            batchSaveChunks(chunks, documentId, knowledgeBaseId);
        } catch (Exception e) {
            rollbackVectorStore(chunks, documentId, e);
            throw e;
        }

        log.info("[ETL] 管道完成, documentId={}, chunks={}, cost={}ms",
                documentId, chunks.size(), System.currentTimeMillis() - start);
        return chunks.size();
    }

    private List<Document> assignStableChunkIds(List<Document> chunks, Map<String, Object> baseMetadata) {
        List<Document> normalizedChunks = new ArrayList<>(chunks.size());
        Long documentId = parseLong(baseMetadata.get("documentId"));
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            Map<String, Object> chunkMetadata = new LinkedHashMap<>(baseMetadata);
            chunkMetadata.putAll(chunk.getMetadata());
            chunkMetadata.put("chunkIndex", i);
            String chunkId = buildChunkId(documentId, i);
            chunkMetadata.put("chunkId", chunkId);
            chunkMetadata.put("vectorId", chunkId);
            normalizedChunks.add(new Document(chunkId, chunk.getText(), chunkMetadata));
        }
        return normalizedChunks;
    }

    /**
     * 根据策略选择分片器
     */
    private List<Document> splitByStrategy(List<Document> documents, String strategy) {
        if ("fixed_length".equalsIgnoreCase(strategy)) {
            return tokenTextSplitter.apply(documents);
        }
        if ("by_title".equalsIgnoreCase(strategy) || "smart".equalsIgnoreCase(strategy)) {
            SmartTextSplitter smartSplitter = new SmartTextSplitter(
                    documentProcessingProperties.getChunkSize(),
                    documentProcessingProperties.getOverlapSize(),
                    documentProcessingProperties.getMaxChunkSize()
            );
            return smartSplitter.split(documents);
        }
        // 默认使用 TokenTextSplitter
        return tokenTextSplitter.apply(documents);
    }

    /**
     * 批量持久化分片
     */
    private void batchSaveChunks(List<Document> chunks, Long documentId, Long knowledgeBaseId) {
        List<DocumentChunk> chunkEntities = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            DocumentChunk entity = new DocumentChunk();
            entity.setDocumentId(documentId);
            entity.setKnowledgeBaseId(knowledgeBaseId);
            entity.setChunkIndex(i);
            entity.setContent(chunk.getText());
            int charCount = chunk.getText() == null ? 0 : chunk.getText().length();
            entity.setWordCount(charCount);
            entity.setTokenCount(estimateTokenCount(chunk.getText()));
            entity.setVectorId(ragDocumentHelper.resolveChunkId(chunk));
            chunkEntities.add(entity);
        }
        // 批量插入（MyBatis-Plus 的 insert 逐条，这里分批次减少事务压力）
        int batchSize = 100;
        for (int i = 0; i < chunkEntities.size(); i += batchSize) {
            int end = Math.min(i + batchSize, chunkEntities.size());
            List<DocumentChunk> batch = chunkEntities.subList(i, end);
            documentChunkMapper.batchInsert(batch);
        }
    }

    private void rollbackVectorStore(List<Document> chunks, Long documentId, Exception cause) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        List<String> chunkIds = chunks.stream()
                .map(Document::getId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (chunkIds.isEmpty()) {
            return;
        }
        try {
            vectorStore.delete(chunkIds);
            log.warn("[ETL] 数据库存储失败，已回滚向量数据, documentId={}, chunkCount={}, error={}",
                    documentId, chunkIds.size(), cause.getMessage());
        } catch (Exception rollbackException) {
            log.error("[ETL] 数据库存储失败且向量回滚失败, documentId={}, error={}",
                    documentId, rollbackException.getMessage(), rollbackException);
        }
    }

    /**
     * 估算 Token 数量（粗略规则：中文按字数，英文按空格分词后的 1.3 倍）
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int chineseChars = 0;
        int otherChars = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chineseChars++;
            } else if (!Character.isWhitespace(c)) {
                otherChars++;
            }
        }
        // 中文: 1 token ≈ 1-2 字, 英文: 1 token ≈ 4 字符
        int chineseTokens = (int) Math.ceil(chineseChars * 0.7);
        int englishTokens = (int) Math.ceil(otherChars / 4.0);
        return chineseTokens + englishTokens;
    }

    private String resolveLocation(String filePath) {
        if (filePath.startsWith("http://") || filePath.startsWith("https://")
                || filePath.startsWith("file:") || filePath.startsWith("classpath:")) {
            return filePath;
        }
        return "file:" + filePath;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String buildChunkId(Long documentId, int chunkIndex) {
        String docPart = documentId == null ? "unknown" : String.valueOf(documentId);
        return docPart + "_" + chunkIndex;
    }

    private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        if (metadata == null || metadata.isEmpty()) {
            return sanitized;
        }
        metadata.forEach((key, value) -> {
            if (key != null && value != null) {
                sanitized.put(key, value);
            }
        });
        return sanitized;
    }
}
