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
import com.stephen.cloud.api.knowledge.model.enums.KnowledgeParseStatusEnum;
import com.stephen.cloud.ai.service.DocumentChunkService;
import com.stephen.cloud.ai.service.EmbeddingVectorService;
import com.stephen.cloud.ai.service.KnowledgeIngestService;
import com.stephen.cloud.ai.service.VectorStoreService;
import com.stephen.cloud.api.knowledge.model.dto.knowledgedocument.KnowledgeDocIngestMessage;
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
     * 执行文档入库完整 ETL 流程 (Extract, Transform, Load)
     * <p>
     * 核心步骤：
     * 1. 原子性更新文档解析状态为 PROCESSING。
     * 2. 物理资源获取：针对 HTTP 地址执行异步下载缓存。
     * 3. 文本提取：调用 DocumentTextExtractor 针对不同 MIME 执行深度解析（PDF/Word/Markdown 等）。
     * 4. 文本清洗：去除冗余字符与乱码，增强向量化质量。
     * 5. 旧数据清理：执行幂等删除，确保相同文档 ID 不产生重复切片。
     * 6. 语义分片：调用 TextChunker 实现带重叠部分的逻辑切分。
     * 7. 向量化索引：双路存储 (MySQL 结构化索引 + ES 向量向量存储 + 向量元数据审计)。
     * </p>
     *
     * @param message 包含文档元数据 ID、物理路径及归属信息的 MQ 消息载体
     */
    @Override
    public void ingestDocument(KnowledgeDocIngestMessage message) {
        if (message == null || message.getDocumentId() == null) {
            return;
        }
        // 1. 获取并检查解析状态，避免重复解析
        KnowledgeDocument doc = knowledgeDocumentMapper.selectById(message.getDocumentId());
        if (doc == null || Integer.valueOf(KnowledgeParseStatusEnum.DONE.getValue()).equals(doc.getParseStatus())) {
            log.info("文档无需解析或记录缺失: docId={}", message.getDocumentId());
            return;
        }

        updateStatus(doc, KnowledgeParseStatusEnum.PROCESSING, null);

        try {
            // 2. 将远程资源下载至本地缓存目录，方便后端解析器读取
            Path localPath = prepareLocalFile(doc, message.getStoragePath());

            log.info("开始提取文本: docId={}", doc.getId());
            // 3. 执行多格式文本提取
            String rawText = DocumentTextExtractor.extract(localPath, localPath.getFileName().toString().toLowerCase());
            log.info("文本提取完成，字符数={}", rawText.length());
            // 4. 内容降噪清洗
            String cleanedText = contentTextCleaner.clean(rawText);
            if (StringUtils.isBlank(cleanedText)) {
                throw new IllegalStateException("解析后的文档内容为空，无法构建索引");
            }

            // 5. 数据治理：清理历史遗留切片与向量审计记录
            removeChunksAndVectorsForDocument(doc.getId());

            // 6. 执行分片策略
            List<String> chunkTexts = textChunker.splitWithOverlap(cleanedText);
            
            // 7. 核心落库流程：关联分片、向量入库与审计
            processAndIndexChunks(doc, chunkTexts);

            // 更新状态为完成
            updateStatus(doc, KnowledgeParseStatusEnum.DONE, null);
            log.info("知识文档入库成功: docId={}, 产生切片数={}", doc.getId(), chunkTexts.size());
        } catch (Exception e) {
            log.error("知识文档入库失败 (docId: {}): {}", doc.getId(), e.getMessage(), e);
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
    @Transactional(rollbackFor = Exception.class)
    protected void processAndIndexChunks(KnowledgeDocument doc, List<String> chunkTexts) {
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
