package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.knowledge.parser.DocumentTextExtractor;
import com.stephen.cloud.ai.knowledge.util.TextChunker;
import com.stephen.cloud.ai.mapper.DocumentChunkMapper;
import com.stephen.cloud.ai.mapper.EmbeddingVectorMapper;
import com.stephen.cloud.ai.mapper.KnowledgeDocumentMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.model.entity.EmbeddingVector;
import com.stephen.cloud.ai.model.entity.KnowledgeDocument;
import com.stephen.cloud.ai.model.enums.KnowledgeParseStatusEnum;
import com.stephen.cloud.ai.service.KnowledgeIngestService;
import com.stephen.cloud.ai.service.VectorStoreService;
import com.stephen.cloud.api.knowledge.model.dto.KnowledgeDocIngestMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KnowledgeIngestServiceImpl implements KnowledgeIngestService {

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private DocumentChunkMapper documentChunkMapper;

    @Resource
    private EmbeddingVectorMapper embeddingVectorMapper;

    @Resource
    private VectorStoreService vectorStoreService;

    @Resource
    private KnowledgeProperties knowledgeProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ingestDocument(KnowledgeDocIngestMessage message) {
        if (message == null || message.getDocumentId() == null) {
            return;
        }
        KnowledgeDocument doc = knowledgeDocumentMapper.selectById(message.getDocumentId());
        if (doc == null) {
            return;
        }
        if (doc.getParseStatus() != null
                && doc.getParseStatus() == KnowledgeParseStatusEnum.DONE.getValue()) {
            return;
        }
        doc.setParseStatus(KnowledgeParseStatusEnum.PROCESSING.getValue());
        doc.setErrorMsg(null);
        knowledgeDocumentMapper.updateById(doc);

        try {
            String storagePath = message.getStoragePath();
            Path localPath;
            boolean isRemote = storagePath.startsWith("http");
            
            if (isRemote) {
                // 如果是远程 URL (COS)，先检查本地持久化缓存
                String cacheDir = knowledgeProperties.getStorageDir() + "/cache";
                FileUtil.mkdir(cacheDir);
                
                // 使用文档 ID 和原始文件名生成稳定的缓存文件名
                String fileName = doc.getId() + "_" + doc.getOriginalName();
                String localFilePath = cacheDir + "/" + fileName;
                File localFile = new File(localFilePath);
                
                if (!localFile.exists()) {
                    log.info("Downloading remote document to cache: docId={}, url={}", doc.getId(), storagePath);
                    HttpUtil.downloadFile(storagePath, localFile);
                } else {
                    log.info("Using cached document: docId={}, path={}", doc.getId(), localFilePath);
                }
                localPath = localFile.toPath();
            } else {
                localPath = Path.of(storagePath);
            }

            String name = localPath.getFileName().toString().toLowerCase();
            String text = DocumentTextExtractor.extract(localPath, name);
            
            // 下方移除自动删除逻辑，实现本地持久化缓存 (按用户建议优化)

            if (StringUtils.isBlank(text)) {
                throw new IllegalStateException("文档无有效文本");
            }

            removeOldChunksAndVectors(doc.getId());

            List<String> parts = TextChunker.splitWithOverlap(text, knowledgeProperties.getChunkSize(),
                    knowledgeProperties.getChunkOverlap());
            List<Document> vectorDocs = new ArrayList<>();
            long kbId = doc.getKnowledgeBaseId();
            long userId = doc.getUserId();
            long docId = doc.getId();

            List<DocumentChunk> savedChunks = new ArrayList<>();
            int idx = 0;
            for (String part : parts) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(docId);
                chunk.setKnowledgeBaseId(kbId);
                chunk.setChunkIndex(idx++);
                chunk.setContent(part);
                chunk.setTokenEstimate(Math.min(part.length(), knowledgeProperties.getChunkSize()));
                documentChunkMapper.insert(chunk);
                savedChunks.add(chunk);

                Map<String, Object> meta = new HashMap<>();
                meta.put("knowledgeBaseId", String.valueOf(kbId));
                meta.put("documentId", String.valueOf(docId));
                meta.put("chunkId", String.valueOf(chunk.getId()));
                meta.put("userId", String.valueOf(userId));

                vectorDocs.add(Document.builder()
                        .id(String.valueOf(chunk.getId()))
                        .text(part)
                        .metadata(meta)
                        .build());
            }

            vectorStoreService.addDocuments(vectorDocs);

            for (DocumentChunk chunk : savedChunks) {
                EmbeddingVector ev = new EmbeddingVector();
                ev.setChunkId(chunk.getId());
                ev.setEmbeddingModel(knowledgeProperties.getEmbeddingModelName());
                ev.setDimension(knowledgeProperties.getEmbeddingDimension());
                ev.setEsDocId(String.valueOf(chunk.getId()));
                embeddingVectorMapper.insert(ev);
            }

            doc.setParseStatus(KnowledgeParseStatusEnum.DONE.getValue());
            doc.setErrorMsg(null);
            knowledgeDocumentMapper.updateById(doc);
        } catch (Exception e) {
            log.error("ingest failed docId={}", message.getDocumentId(), e);
            doc.setParseStatus(KnowledgeParseStatusEnum.FAILED.getValue());
            String msg = e.getMessage();
            if (msg != null && msg.length() > 2000) {
                msg = msg.substring(0, 2000);
            }
            doc.setErrorMsg(msg);
            knowledgeDocumentMapper.updateById(doc);
        }
    }

    private void removeOldChunksAndVectors(long documentId) {
        vectorStoreService.deleteByDocumentId(documentId);
        LambdaQueryWrapper<DocumentChunk> qw = new LambdaQueryWrapper<>();
        qw.eq(DocumentChunk::getDocumentId, documentId);
        List<DocumentChunk> old = documentChunkMapper.selectList(qw);
        for (DocumentChunk c : old) {
            embeddingVectorMapper.delete(new LambdaQueryWrapper<EmbeddingVector>()
                    .eq(EmbeddingVector::getChunkId, c.getId()));
        }
        documentChunkMapper.delete(qw);
    }
}
