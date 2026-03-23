package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.knowledge.etl.ContentTextCleaner;
import com.stephen.cloud.ai.knowledge.parser.DocumentTextExtractor;
import com.stephen.cloud.ai.knowledge.util.TextChunker;
import com.stephen.cloud.ai.mapper.KnowledgeDocumentMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
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
 * 知识入库服务实现类：负责文档的完整 ETL (Extract, Transform, Load) 流水线。
 * <p>
 * 该服务集成 Spring AI 的能力，将非结构化文档转换为向量化分片并存储。
 * 核心流程：
 * 1. 资源准备：从本地或远程同步文档。
 * 2. 文本提取：使用 Tika 等工具提取原始文本，并进行清洗。
 * 3. 语义分片：通过 TextChunker 进行智能切分。
 * 4. 向量化入库：双路并发处理（MySQL 分片索引 + Elasticsearch 向量存储 + 过程审计）。
 * </p>
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class KnowledgeIngestServiceImpl implements KnowledgeIngestService {

    /**
     * 文档元数据 Mapper：维护文档解析状态
     */
    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    /**
     * 分片持久化服务：存储文本分片到 MySQL
     */
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
     * 执行文档入库主流程（由消息中心调用的入库入口）。
     *
     * @param message 包含文档 ID 和存储路径的消息对象
     */
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

            // 3. 幂等性清理 旧分片：确保重新解析时不会产生脏数据
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

    /**
     * 将解析后的文本分片持久化到 MySQL 和向量存储。
     *
     * @param doc        文档元数据对象
     * @param chunkTexts 切分后的文本分片列表
     */
    private void processAndIndexChunks(KnowledgeDocument doc, List<String> chunkTexts) {
        long kbId = doc.getKnowledgeBaseId();
        long docId = doc.getId();

        // 1. 调用分片服务：批量构建并保存文本分片 (MySQL)
        List<DocumentChunk> chunksToSave = documentChunkService.batchCreateChunks(
                docId, kbId, chunkTexts, knowledgeProperties.getChunkSize());

        if (chunksToSave.isEmpty()) {
            return;
        }

        // 2. 构造向量库 Document 对象及审计日志数据
        List<Document> vectorDocs = new ArrayList<>();
        List<String> esDocIds = new ArrayList<>();

        for (DocumentChunk chunk : chunksToSave) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("knowledgeBaseId", String.valueOf(kbId));
            metadata.put("documentId", String.valueOf(docId));
            metadata.put("documentName", doc.getOriginalName());
            metadata.put("chunkId", String.valueOf(chunk.getId()));
            metadata.put("userId", String.valueOf(doc.getUserId()));

            String chunkIdStr = String.valueOf(chunk.getId());
            vectorDocs.add(Document.builder()
                    .id(chunkIdStr)
                    .text(chunk.getContent())
                    .metadata(metadata)
                    .build());
            esDocIds.add(chunkIdStr);
        }

        // 3. 并行写入：向量库存储 + 过程审计日志
        vectorStoreService.addDocuments(vectorDocs);
        embeddingVectorService.batchSaveAuditLogs(
                chunksToSave,
                knowledgeProperties.getEmbeddingModelName(),
                knowledgeProperties.getEmbeddingDimension(),
                esDocIds
        );
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

    /**
     * 清理文档对应的所有旧数据（分片、向量及审计日志），保证入库幂等性。
     *
     * @param documentId 文档内码
     */
    @Override
    public void removeChunksAndVectorsForDocument(long documentId) {
        log.info("Cleaning up all data for documentId: {}", documentId);
        // 1. 清理向量库中的分片 (Elasticsearch)
        vectorStoreService.deleteByDocumentId(documentId);
        // 2. 清理向量元数据审计记录 (MySQL)
        embeddingVectorService.deleteByDocumentId(documentId);
        // 3. 清理原始文本分片 (MySQL)
        documentChunkService.deleteByDocumentId(documentId);
    }
}
