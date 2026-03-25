package com.stephen.cloud.ai.knowledge.etl;

import com.stephen.cloud.ai.knowledge.reader.DocumentReaderFactory;
import com.stephen.cloud.ai.mapper.DocumentChunkMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.config.DocumentProcessingProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public int process(String filePath, String fileExtension, Map<String, Object> metadata) {
        long start = System.currentTimeMillis();
        String location = resolveLocation(filePath);
        org.springframework.core.io.Resource springResource = resourceLoader.getResource(location);
        DocumentReader reader = documentReaderFactory.getReader(fileExtension, springResource);
        List<Document> documents = reader.get();

        // 注入元数据
        for (Document document : documents) {
            document.getMetadata().putAll(metadata);
        }

        // 根据策略选择分片器
        String strategy = documentProcessingProperties.getChunkStrategy();
        List<Document> chunks = splitByStrategy(documents, strategy);
        log.info("[ETL] 分片完成, strategy={}, chunkCount={}, fileExtension={}",
                strategy, chunks.size(), fileExtension);

        // 设置分片索引和 chunkId
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            chunk.getMetadata().put("chunkIndex", i);
            String chunkId = metadata.get("documentId") + "_" + i + "_" + UUID.randomUUID();
            chunk.getMetadata().put("chunkId", chunkId);
        }

        // 写入向量存储
        vectorStore.add(chunks);

        // 批量持久化分片到数据库
        Long documentId = parseLong(metadata.get("documentId"));
        Long knowledgeBaseId = parseLong(metadata.get("knowledgeBaseId"));
        batchSaveChunks(chunks, documentId, knowledgeBaseId);

        log.info("[ETL] 管道完成, documentId={}, chunks={}, cost={}ms",
                documentId, chunks.size(), System.currentTimeMillis() - start);
        return chunks.size();
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
            entity.setVectorId(chunk.getId());
            chunkEntities.add(entity);
        }
        // 批量插入（MyBatis-Plus 的 insert 逐条，这里分批次减少事务压力）
        int batchSize = 100;
        for (int i = 0; i < chunkEntities.size(); i += batchSize) {
            int end = Math.min(i + batchSize, chunkEntities.size());
            List<DocumentChunk> batch = chunkEntities.subList(i, end);
            for (DocumentChunk entity : batch) {
                documentChunkMapper.insert(entity);
            }
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
}
