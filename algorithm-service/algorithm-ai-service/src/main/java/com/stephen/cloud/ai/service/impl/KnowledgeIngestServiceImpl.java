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
 * 知识入库服务实现：支撑排序算法教学等场景下上传或种子生成的文档进入向量库。
 * <p>
 * 下载或定位文件 → {@link com.stephen.cloud.ai.knowledge.parser.DocumentTextExtractor} →
 * {@link com.stephen.cloud.ai.knowledge.etl.ContentTextCleaner} → 清理旧分片 →
 * {@link com.stephen.cloud.ai.knowledge.util.TextChunker} 分片 → 写 {@code document_chunk} →
 * {@link VectorStoreService} 写 ES → 写 {@code embedding_vector}；不在方法上加本地事务，避免与 ES 写语义混淆。
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

    /**
     * 解析并入库文档：更新状态为处理中，失败时写入 {@link KnowledgeDocument#errorMsg}
     *
     * @param message MQ 消息体
     */
    @Override
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
            // 1. 解析存储路径：远程 URL 落本地缓存目录，本地路径直接读取
            String storagePath = message.getStoragePath();
            Path localPath;
            boolean isRemote = storagePath.startsWith("http");

            if (isRemote) {
                String cacheDir = knowledgeProperties.getStorageDir() + "/cache";
                FileUtil.mkdir(cacheDir);

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

            // 2. 按后缀提取文本（Tika / CSV 等）
            String name = localPath.getFileName().toString().toLowerCase();
            String text = DocumentTextExtractor.extract(localPath, name);
            text = contentTextCleaner.clean(text);

            if (StringUtils.isBlank(text)) {
                throw new IllegalStateException("文档无有效文本");
            }

            // 3. 清理该文档历史分片与向量（幂等重跑）
            removeChunksAndVectorsForDocument(doc.getId());

            // 4. 分片并批量保存 document_chunk，再构建 Spring AI Document 写入向量库
            List<String> parts = textChunker.splitWithOverlap(text);
            List<Document> vectorDocs = new ArrayList<>();
            long kbId = doc.getKnowledgeBaseId();
            long userId = doc.getUserId();
            long docId = doc.getId();

            List<DocumentChunk> chunksToSave = new ArrayList<>();
            int idx = 0;
            for (String part : parts) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(docId);
                chunk.setKnowledgeBaseId(kbId);
                chunk.setChunkIndex(idx++);
                chunk.setContent(part);
                chunk.setTokenEstimate(Math.min(part.length(), knowledgeProperties.getChunkSize()));
                chunksToSave.add(chunk);
            }

            if (!chunksToSave.isEmpty()) {
                documentChunkService.saveBatch(chunksToSave);
            }

            for (DocumentChunk chunk : chunksToSave) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("knowledgeBaseId", String.valueOf(kbId));
                meta.put("documentId", String.valueOf(docId));
                meta.put("documentName", doc.getOriginalName());
                meta.put("chunkId", String.valueOf(chunk.getId()));
                meta.put("userId", String.valueOf(userId));

                vectorDocs.add(Document.builder()
                        .id(String.valueOf(chunk.getId()))
                        .text(chunk.getContent())
                        .metadata(meta)
                        .build());
            }

            vectorStoreService.addDocuments(vectorDocs);

            // 5. 批量写入 embedding_vector 元数据（与 ES 文档 id 对齐）
            List<EmbeddingVector> embeddingRows = new ArrayList<>();
            for (DocumentChunk chunk : chunksToSave) {
                EmbeddingVector ev = new EmbeddingVector();
                ev.setChunkId(chunk.getId());
                ev.setEmbeddingModel(knowledgeProperties.getEmbeddingModelName());
                ev.setDimension(knowledgeProperties.getEmbeddingDimension());
                ev.setEsDocId(String.valueOf(chunk.getId()));
                embeddingRows.add(ev);
            }
            if (!embeddingRows.isEmpty()) {
                embeddingVectorService.saveBatch(embeddingRows);
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

    /**
     * 删除 ES 中该文档向量及 MySQL 中分片、向量元数据行
     *
     * @param documentId 文档 ID
     */
    @Override
    public void removeChunksAndVectorsForDocument(long documentId) {
        // 1. 按 metadata 删除 ES 文档
        vectorStoreService.deleteByDocumentId(documentId);
        LambdaQueryWrapper<DocumentChunk> qw = new LambdaQueryWrapper<>();
        qw.eq(DocumentChunk::getDocumentId, documentId);
        List<DocumentChunk> old = documentChunkService.list(qw);
        // 2. 先删 embedding_vector（按 chunkId），再删 document_chunk
        for (DocumentChunk c : old) {
            embeddingVectorService.remove(new LambdaQueryWrapper<EmbeddingVector>()
                    .eq(EmbeddingVector::getChunkId, c.getId()));
        }
        documentChunkService.remove(qw);
    }
}
