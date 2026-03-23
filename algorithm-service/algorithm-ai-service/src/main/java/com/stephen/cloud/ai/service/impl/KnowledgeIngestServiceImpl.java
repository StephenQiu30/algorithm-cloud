package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.knowledge.processor.ContentTextCleaner;
import com.stephen.cloud.ai.knowledge.processor.DocumentTextExtractor;
import com.stephen.cloud.ai.knowledge.processor.TextChunker;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.model.entity.KnowledgeDocument;
import com.stephen.cloud.ai.mq.KnowledgeIngestMqProducer;
import com.stephen.cloud.ai.service.DocumentChunkService;
import com.stephen.cloud.ai.service.KnowledgeDocumentService;
import com.stephen.cloud.ai.service.KnowledgeIngestService;
import com.stephen.cloud.api.knowledge.model.dto.knowledgedocument.KnowledgeDocIngestMessage;
import com.stephen.cloud.api.knowledge.model.enums.KnowledgeParseStatusEnum;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

/**
 * 知识文档解析入库服务实现类。
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class KnowledgeIngestServiceImpl implements KnowledgeIngestService {

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;
    @Resource
    private DocumentChunkService documentChunkService;
    @Resource
    private KnowledgeProperties knowledgeProperties;
    @Resource
    private VectorStore knowledgeVectorStore;
    @Resource
    private TextChunker textChunker;
    @Resource
    private ContentTextCleaner contentTextCleaner;
    @Resource
    private CacheUtils cacheUtils;
    @Resource
    private KnowledgeIngestMqProducer knowledgeIngestMqProducer;

    private final FilterExpressionTextParser filterParser = new FilterExpressionTextParser();

    @Override
    public void ingestDocument(KnowledgeDocIngestMessage message) {
        if (message == null || message.getDocumentId() == null) {
            log.warn("[文档入库] 收到空或无效的入库消息");
            return;
        }
        KnowledgeDocument doc = knowledgeDocumentService.getById(message.getDocumentId());
        if (doc == null) {
            log.error("[文档入库] 未找到文档: id={}", message.getDocumentId());
            return;
        }
        if (Objects.equals(doc.getParseStatus(), KnowledgeParseStatusEnum.DONE.getValue())) {
            log.info("[文档入库] 文档已处理完成，跳过: id={}, name={}", doc.getId(), doc.getOriginalName());
            return;
        }

        log.info("[文档入库] 开始执行文档入库流水线: id={}, name={}", doc.getId(), doc.getOriginalName());
        updateDocumentStatus(doc, KnowledgeParseStatusEnum.PROCESSING, null);
        
        try {
            List<Document> chunkDocs;
            String cacheKey = "kb:parsed:" + doc.getId();
            chunkDocs = cacheUtils.get(cacheKey);
            
            if (chunkDocs == null || chunkDocs.isEmpty()) {
                log.info("[文档入库] 未发现缓存，开始完整解析流程: doc={}", doc.getId());
                
                log.info("[文档入库] 阶段 1: 准备本地文件: doc={}", doc.getId());
                Path localPath = prepareLocalFile(doc, message.getStoragePath());
                
                log.info("[文档入库] 阶段 2: 提取文件文本内容: doc={}", doc.getId());
                List<Document> extracted = DocumentTextExtractor.readDocuments(localPath, localPath.getFileName().toString().toLowerCase());
                if (extracted.isEmpty()) {
                    throw new IllegalStateException("文本提取阶段返回内容为空");
                }
                
                log.info("[文档入库] 阶段 3: 清洗文本并丰富元数据: doc={}", doc.getId());
                Map<String, Object> commonMeta = createCommonMetadata(doc);
                List<Document> enriched = enrichMetadata(extracted, commonMeta);
                List<Document> cleaned = contentTextCleaner.apply(enriched);
                
                log.info("[文档入库] 阶段 4: 文本分段切块: doc={}", doc.getId());
                chunkDocs = textChunker.splitToChunkDocuments(cleaned);
                if (chunkDocs.isEmpty()) {
                    throw new IllegalStateException("文本切分阶段返回分块为空");
                }
                
                cacheUtils.put(cacheKey, chunkDocs, 3600 * 24L);
                log.info("[文档入库] 文本解析完成，已缓存 {} 个分块: doc={}", chunkDocs.size(), doc.getId());
            } else {
                log.info("[文档入库] 使用已缓存的解析结果 ({} 个分块): doc={}", chunkDocs.size(), doc.getId());
            }

            log.info("[文档入库] 阶段 5: 将数据加载至向量库和数据库: doc={}", doc.getId());
            handleLoadPhase(doc, chunkDocs);
            
            updateDocumentStatus(doc, KnowledgeParseStatusEnum.DONE, null);
            log.info("[文档入库] 文档入库流水线执行成功: id={}, name={}", doc.getId(), doc.getOriginalName());
        } catch (Exception e) {
            log.error("[文档入库] 任务执行失败: id={}, name={}, error={}", 
                    doc.getId(), doc.getOriginalName(), e.getMessage(), e);
            updateDocumentStatus(doc, KnowledgeParseStatusEnum.FAILED, e.getMessage());
        }
    }

    @Override
    public void retryIngest(Long documentId) {
        if (documentId == null || documentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文档 ID 非法");
        }
        KnowledgeDocument doc = knowledgeDocumentService.getById(documentId);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文档不存在");
        }
        doc.setParseStatus(KnowledgeParseStatusEnum.PENDING.getValue());
        doc.setErrorMsg(null);
        knowledgeDocumentService.updateById(doc);
        cacheUtils.delete("kb:parsed:" + documentId);
        knowledgeIngestMqProducer.sendIngestCreated(KnowledgeDocIngestMessage.builder()
                .documentId(doc.getId())
                .knowledgeBaseId(doc.getKnowledgeBaseId())
                .userId(doc.getUserId())
                .storagePath(doc.getStoragePath())
                .build());
        log.info("[文档入库] 已重新投递入库任务: docId={}, kbId={}", doc.getId(), doc.getKnowledgeBaseId());
    }

    @Transactional(rollbackFor = Exception.class)
    protected void handleLoadPhase(KnowledgeDocument doc, List<Document> chunkDocs) {
        log.info("[数据加载] 清理旧数据: doc={}", doc.getId());
        knowledgeVectorStore.delete(filterParser.parse("documentId == '" + doc.getId() + "'"));
        documentChunkService.deleteByDocumentId(doc.getId());
        
        IntSummaryStatistics stats = chunkDocs.stream()
                .map(Document::getText)
                .filter(Objects::nonNull)
                .mapToInt(String::length)
                .summaryStatistics();
        long shortChunkCount = chunkDocs.stream()
                .map(Document::getText)
                .filter(Objects::nonNull)
                .filter(t -> t.length() < Math.max(50, knowledgeProperties.getChunkSize() / 5))
                .count();
        log.info("[数据加载] 分片质量基线: doc={}, chunkCount={}, minLen={}, avgLen={}, maxLen={}, shortChunkCount={}",
                doc.getId(),
                chunkDocs.size(),
                stats.getCount() > 0 ? stats.getMin() : 0,
                stats.getCount() > 0 ? Math.round(stats.getAverage()) : 0,
                stats.getCount() > 0 ? stats.getMax() : 0,
                shortChunkCount);

        log.info("[数据加载] 将 {} 个分块持久化至数据库: doc={}", chunkDocs.size(), doc.getId());
        List<DocumentChunk> dbChunks = new ArrayList<>();
        for (int i = 0; i < chunkDocs.size(); i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(doc.getId());
            chunk.setKnowledgeBaseId(doc.getKnowledgeBaseId());
            chunk.setChunkIndex(i);
            chunk.setContent(chunkDocs.get(i).getText());
            chunk.setTokenEstimate(Math.min(chunkDocs.get(i).getText().length(), knowledgeProperties.getChunkSize()));
            dbChunks.add(chunk);
        }
        documentChunkService.saveBatch(dbChunks);
        
        log.info("[数据加载] 构建并写入向量库: doc={}", doc.getId());
        List<Document> vectorDocs = new ArrayList<>();
        for (int i = 0; i < dbChunks.size(); i++) {
            DocumentChunk dbChunk = dbChunks.get(i);
            Map<String, Object> meta = new HashMap<>(chunkDocs.get(i).getMetadata());
            meta.put("chunkId", String.valueOf(dbChunk.getId()));
            vectorDocs.add(new Document(String.valueOf(dbChunk.getId()), dbChunk.getContent(), meta));
        }

        int batchSize = knowledgeProperties.getVectorBatchSize();
        for (int i = 0; i < vectorDocs.size(); i += batchSize) {
            int end = Math.min(i + batchSize, vectorDocs.size());
            List<Document> batch = vectorDocs.subList(i, end);
            try {
                knowledgeVectorStore.add(batch);
                log.debug("[数据加载] 批量写入进度: {}/{} for doc={}", end, vectorDocs.size(), doc.getId());
            } catch (Exception e) {
                log.error("[数据加载] 向量批量写入失败: docId={}, index={}", doc.getId(), i, e);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "向量库写入异常: " + e.getMessage());
            }
        }
        log.info("[数据加载] 向量加载完成，共 {} 条: doc={}", vectorDocs.size(), doc.getId());
    }

    private Path prepareLocalFile(KnowledgeDocument doc, String storagePath) {
        String fileName = doc.getId() + "_" + doc.getOriginalName();
        File libraryFile = new File(knowledgeProperties.getLocalLibraryDir(), fileName);
        if (libraryFile.exists()) {
            log.debug("[准备文件] 本地文件已存在: {}", libraryFile.getAbsolutePath());
            return libraryFile.toPath();
        }
        if (storagePath.startsWith("http")) {
            log.info("[准备文件] 从云端存储下载文件: {}", storagePath);
            FileUtil.mkdir(knowledgeProperties.getLocalLibraryDir());
            HttpUtil.downloadFile(storagePath, libraryFile);
            return libraryFile.toPath();
        }
        return Path.of(storagePath);
    }

    private Map<String, Object> createCommonMetadata(KnowledgeDocument doc) {
        Map<String, Object> m = new HashMap<>();
        m.put("knowledgeBaseId", String.valueOf(doc.getKnowledgeBaseId()));
        m.put("documentId", String.valueOf(doc.getId()));
        m.put("documentName", doc.getOriginalName());
        m.put("userId", String.valueOf(doc.getUserId()));
        return m;
    }

    private List<Document> enrichMetadata(List<Document> docs, Map<String, Object> meta) {
        return docs.stream().map(d -> {
            Map<String, Object> combined = new HashMap<>(meta);
            if (d.getMetadata() != null) combined.putAll(d.getMetadata());
            return new Document(d.getText(), combined);
        }).toList();
    }

    private void updateDocumentStatus(KnowledgeDocument doc, KnowledgeParseStatusEnum status, String error) {
        doc.setParseStatus(status.getValue());
        if (error != null) doc.setErrorMsg(error.length() > 2000 ? error.substring(0, 2000) : error);
        knowledgeDocumentService.updateById(doc);
    }

    @Override
    public void deleteVectors(Long documentId) {
        if (documentId == null) return;
        knowledgeVectorStore.delete(filterParser.parse("documentId == '" + documentId + "'"));
    }
}
