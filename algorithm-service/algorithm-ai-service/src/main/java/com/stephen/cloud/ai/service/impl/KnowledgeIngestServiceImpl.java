package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.knowledge.etl.ContentTextCleaner;
import com.stephen.cloud.ai.knowledge.parser.DocumentTextExtractor;
import com.stephen.cloud.ai.knowledge.util.TextChunker;
import com.stephen.cloud.ai.mapper.KnowledgeDocumentMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.model.entity.EmbeddingVector;
import com.stephen.cloud.ai.model.entity.KnowledgeDocument;
import com.stephen.cloud.ai.model.enums.KnowledgeParseStatusEnum;
import com.stephen.cloud.ai.service.DocumentChunkService;
import com.stephen.cloud.ai.service.EmbeddingVectorService;
import com.stephen.cloud.ai.service.KnowledgeIngestService;
import com.stephen.cloud.ai.service.VectorStoreService;
import com.stephen.cloud.api.knowledge.model.dto.KnowledgeDocIngestMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识入库服务实现：集成 Spring AI 的解析与分片能力。
 * <p>
 * 遵循 Lean Architecture 原则，移除冗余的 MySQL Embedding 镜像表，直接依赖向量存储。
 * </p>
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class KnowledgeIngestServiceImpl implements KnowledgeIngestService {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private DocumentChunkService documentChunkService;

    @Resource
    private EmbeddingVectorService embeddingVectorService;

    @Resource
    private VectorStoreService vectorStoreService;

    @Resource
    private KnowledgeProperties knowledgeProperties;

    @Resource
    private TextChunker textChunker;

    @Resource
    private ContentTextCleaner contentTextCleaner;

    @Override
    public void ingestDocument(KnowledgeDocIngestMessage message) {
        if (message == null || message.getDocumentId() == null) {
            return;
        }
        KnowledgeDocument doc = knowledgeDocumentMapper.selectById(message.getDocumentId());
        if (doc == null || (doc.getParseStatus() != null && doc.getParseStatus() == KnowledgeParseStatusEnum.DONE.getValue())) {
            return;
        }

        updateStatus(doc, KnowledgeParseStatusEnum.PROCESSING, null);

        try {
            // 1. 资源准备（本地/远程同步）
            Path localPath = prepareLocalFile(doc, message.getStoragePath());

            // 2. 文本提取与预处理
            String rawText = DocumentTextExtractor.extract(localPath, localPath.getFileName().toString().toLowerCase());
            String cleanedText = contentTextCleaner.clean(rawText);
            if (StringUtils.isBlank(cleanedText)) {
                throw new IllegalStateException("文档提取后的文本内容为空");
            }

            // 3. 幂等性清理 旧分片
            removeChunksAndVectorsForDocument(doc.getId());

            // 4. 执行语义分片
            List<String> chunkTexts = textChunker.splitWithOverlap(cleanedText);
            
            // 5. 数据持久化 (MySQL 分片索引 + 向量库)
            processAndIndexChunks(doc, chunkTexts);

            updateStatus(doc, KnowledgeParseStatusEnum.DONE, null);
            log.info("Document ingested successfully: docId={}", doc.getId());
        } catch (Exception e) {
            log.error("Ingestion failed for docId={}", doc.getId(), e);
            updateStatus(doc, KnowledgeParseStatusEnum.FAILED, e.getMessage());
        }
    }

    private Path prepareLocalFile(KnowledgeDocument doc, String storagePath) {
        if (storagePath.startsWith("http")) {
            String cacheDir = knowledgeProperties.getStorageDir() + "/cache";
            FileUtil.mkdir(cacheDir);
            String localFilePath = cacheDir + "/" + doc.getId() + "_" + doc.getOriginalName();
            File localFile = new File(localFilePath);
            if (!localFile.exists()) {
                HttpUtil.downloadFile(storagePath, localFile);
            }
            return localFile.toPath();
        }
        return Path.of(storagePath);
    }

    private void processAndIndexChunks(KnowledgeDocument doc, List<String> chunkTexts) {
        List<DocumentChunk> chunksToSave = new ArrayList<>();
        List<Document> vectorDocs = new ArrayList<>();
        List<EmbeddingVector> embeddingRows = new ArrayList<>();

        long kbId = doc.getKnowledgeBaseId();
        long docId = doc.getId();

        for (int i = 0; i < chunkTexts.size(); i++) {
            String content = chunkTexts.get(i);
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(docId);
            chunk.setKnowledgeBaseId(kbId);
            chunk.setChunkIndex(i);
            chunk.setContent(content);
            chunk.setTokenEstimate(Math.min(content.length(), knowledgeProperties.getChunkSize()));
            chunksToSave.add(chunk);
        }

        if (!chunksToSave.isEmpty()) {
            documentChunkService.saveBatch(chunksToSave);
            
            for (DocumentChunk chunk : chunksToSave) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("knowledgeBaseId", String.valueOf(kbId));
                metadata.put("documentId", String.valueOf(docId));
                metadata.put("documentName", doc.getOriginalName());
                metadata.put("chunkId", String.valueOf(chunk.getId()));
                metadata.put("userId", String.valueOf(doc.getUserId()));

                vectorDocs.add(Document.builder()
                        .id(String.valueOf(chunk.getId()))
                        .text(chunk.getContent())
                        .metadata(metadata)
                        .build());

                // 记录向量元数据，支撑离线 ETL 过程审计
                EmbeddingVector ev = new EmbeddingVector();
                ev.setChunkId(chunk.getId());
                ev.setEmbeddingModel(knowledgeProperties.getEmbeddingModelName());
                ev.setDimension(knowledgeProperties.getEmbeddingDimension());
                ev.setEsDocId(String.valueOf(chunk.getId()));
                embeddingRows.add(ev);
            }
            
            // 写入向量库
            vectorStoreService.addDocuments(vectorDocs);
            // 写入向量审计表
            embeddingVectorService.saveBatch(embeddingRows);
        }
    }

    private void updateStatus(KnowledgeDocument doc, KnowledgeParseStatusEnum status, String errorMsg) {
        doc.setParseStatus(status.getValue());
        if (errorMsg != null) {
            doc.setErrorMsg(errorMsg.length() > 2000 ? errorMsg.substring(0, 2000) : errorMsg);
        } else {
            doc.setErrorMsg(null);
        }
        knowledgeDocumentMapper.updateById(doc);
    }

    @Override
    public void removeChunksAndVectorsForDocument(long documentId) {
        // 同步清理向量库相关分片
        vectorStoreService.deleteByDocumentId(documentId);
        
        LambdaQueryWrapper<DocumentChunk> chunkQw = new LambdaQueryWrapper<>();
        chunkQw.eq(DocumentChunk::getDocumentId, documentId);
        List<DocumentChunk> oldChunks = documentChunkService.list(chunkQw);
        
        if (!oldChunks.isEmpty()) {
            List<Long> chunkIds = oldChunks.stream().map(DocumentChunk::getId).toList();
            // 清理向量元数据审计表
            embeddingVectorService.remove(new LambdaQueryWrapper<EmbeddingVector>()
                    .in(EmbeddingVector::getChunkId, chunkIds));
            documentChunkService.remove(chunkQw);
        }
    }
}
