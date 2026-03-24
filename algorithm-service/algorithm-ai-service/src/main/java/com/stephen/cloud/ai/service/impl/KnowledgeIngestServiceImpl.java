package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.enums.RagMetricEnum;
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
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
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
    @Resource
    private ChatModel chatModel;
    @Resource
    private MeterRegistry meterRegistry;

    private final FilterExpressionTextParser filterParser = new FilterExpressionTextParser();

    private static final String CACHE_KEY_PREFIX = "kb:parsed:";
    private static final int MAX_VECTOR_WRITE_RETRIES = 3;

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
            String cacheKey = CACHE_KEY_PREFIX + doc.getId();
            chunkDocs = cacheUtils.get(cacheKey);

            if (chunkDocs == null || chunkDocs.isEmpty()) {
                log.info("[文档入库] 未发现缓存，开始完整解析流程: doc={}", doc.getId());

                log.info("[文档入库] 阶段 1: 准备本地文件: doc={}", doc.getId());
                Path localPath = prepareLocalFile(doc, message.getStoragePath());

                log.info("[文档入库] 阶段 2: 提取文件文本内容: doc={}", doc.getId());
                List<Document> extracted = DocumentTextExtractor.readDocuments(localPath, localPath.getFileName().toString().toLowerCase());
                if (extracted == null || extracted.isEmpty()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "文本提取失败或文件内容为空");
                }

                log.info("[文档入库] 阶段 3: 清洗文本并丰富元数据: doc={}", doc.getId());
                Map<String, Object> commonMeta = createCommonMetadata(doc);
                List<Document> enriched = enrichMetadata(extracted, commonMeta);
                List<Document> cleaned = contentTextCleaner.apply(enriched);

                log.info("[文档入库] 阶段 4: 文本分段切块: doc={}", doc.getId());
                List<Document> chunks = textChunker.splitToChunkDocuments(cleaned);
                if (chunks == null || chunks.isEmpty()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "文本切分失败或切分结果为空");
                }

                log.info("[文档入库] 阶段 5: 使用 KeywordMetadataEnricher 增强关键词: doc={}", doc.getId());
                chunkDocs = applyKeywordMetadata(chunks, doc.getId());

                cacheUtils.put(cacheKey, chunkDocs, 3600 * 24L);
                log.info("[文档入库] 文本解析完成，已缓存 {} 个分块: doc={}", chunkDocs.size(), doc.getId());
            } else {
                log.info("[文档入库] 使用已缓存的解析结果 ({} 个分块): doc={}", chunkDocs.size(), doc.getId());
            }

            log.info("[文档入库] 阶段 6: 将数据加载至向量库和数据库: doc={}", doc.getId());
            handleLoadPhase(doc, chunkDocs);

            // 事务提交后，异步写入向量库
            log.info("[文档入库] 阶段 7: 写入向量库: doc={}", doc.getId());
            List<Document> vectorDocs = buildVectorDocuments(doc, chunkDocs);
            writeToVectorStore(vectorDocs, doc.getId());

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
        cacheUtils.delete(CACHE_KEY_PREFIX + documentId);
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
        int totalTokens = 0;

        // 从文档继承标签和代码标识
        String docTags = doc.getTags();
        Integer docHasCode = doc.getHasCode();

        for (int i = 0; i < chunkDocs.size(); i++) {
            Document chunkDoc = chunkDocs.get(i);
            String content = chunkDoc.getText();
            int tokenEst = estimateToken(content);
            totalTokens += tokenEst;

            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(doc.getId());
            chunk.setKnowledgeBaseId(doc.getKnowledgeBaseId());
            chunk.setChunkIndex(i);
            chunk.setContent(content);
            chunk.setTokenEstimate(tokenEst);
            chunk.setCharCount(content.length());

            // 继承文档级别的标签和代码标识
            if (StringUtils.isNotBlank(docTags)) {
                chunk.setTags(docTags);
            }
            if (docHasCode != null) {
                chunk.setHasCode(docHasCode);
            }

            // 提取关键词元数据
            Map<String, Object> meta = chunkDoc.getMetadata();
            if (meta != null && meta.containsKey("excerpt_keywords")) {
                Map<String, Object> jsonMeta = new HashMap<>();
                jsonMeta.put("excerpt_keywords", meta.get("excerpt_keywords"));
                chunk.setMetadataJson(JSONUtil.toJsonStr(jsonMeta));
            }

            dbChunks.add(chunk);
        }
        documentChunkService.saveBatch(dbChunks);

        // 更新文档统计信息
        doc.setChunkCount(dbChunks.size());
        doc.setTotalTokens(totalTokens);
        doc.setTotalChars(dbChunks.stream().mapToInt(DocumentChunk::getCharCount).sum());
        knowledgeDocumentService.updateById(doc);

        log.info("[数据加载] 数据库操作完成: doc={}", doc.getId());
    }

    /**
     * 构建向量文档列表
     */
    private List<Document> buildVectorDocuments(KnowledgeDocument doc, List<Document> chunkDocs) {
        // 需要先查询数据库获取已保存的分片ID
        List<DocumentChunk> dbChunks = documentChunkService.lambdaQuery()
                .eq(DocumentChunk::getDocumentId, doc.getId())
                .orderByAsc(DocumentChunk::getChunkIndex)
                .list();

        if (dbChunks.size() != chunkDocs.size()) {
            log.error("[数据加载] 数据库分片数量与内存分片数量不一致: dbSize={}, memSize={}",
                    dbChunks.size(), chunkDocs.size());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "分片数据不一致");
        }

        log.info("[数据加载] 构建向量文档（包含标签和元数据）: doc={}", doc.getId());
        List<Document> vectorDocs = new ArrayList<>();
        for (int i = 0; i < dbChunks.size(); i++) {
            DocumentChunk dbChunk = dbChunks.get(i);

            // 合并所有元数据到向量文档
            Map<String, Object> meta = new HashMap<>(chunkDocs.get(i).getMetadata());
            meta.put("chunkId", String.valueOf(dbChunk.getId()));

            // 添加数据库字段到向量元数据（用于 BM25 检索）
            if (StringUtils.isNotBlank(dbChunk.getTags())) {
                meta.put("tags", dbChunk.getTags());
                // 拆分标签为数组，便于 ES 检索
                meta.put("tag_list", Arrays.asList(dbChunk.getTags().split(",")));
            }
            if (dbChunk.getHasCode() != null && dbChunk.getHasCode() == 1) {
                meta.put("has_code", true);
            }
            if (dbChunk.getCharCount() != null) {
                meta.put("char_count", dbChunk.getCharCount());
            }

            vectorDocs.add(new Document(String.valueOf(dbChunk.getId()), dbChunk.getContent(), meta));
        }

        return vectorDocs;
    }

    /**
     * 向量库写入（事务外执行，带重试机制）
     */
    private void writeToVectorStore(List<Document> vectorDocs, Long docId) {
        log.info("[数据加载] 开始写入向量库: doc={}, 向量数={}", docId, vectorDocs.size());

        // 先清理旧向量（幂等性保证）
        try {
            knowledgeVectorStore.delete(filterParser.parse("documentId == '" + docId + "'"));
            log.debug("[数据加载] 已清理旧向量: doc={}", docId);
        } catch (Exception e) {
            log.error("[数据加载] 清理旧向量失败: doc={}, error={}", docId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "向量库清理失败: " + e.getMessage());
        }

        int batchSize = knowledgeProperties.getVectorBatchSize();
        for (int i = 0; i < vectorDocs.size(); i += batchSize) {
            int end = Math.min(i + batchSize, vectorDocs.size());
            List<Document> batch = vectorDocs.subList(i, end);

            boolean success = false;
            Exception lastException = null;

            // 重试机制
            for (int attempt = 1; attempt <= MAX_VECTOR_WRITE_RETRIES; attempt++) {
                try {
                    knowledgeVectorStore.add(batch);
                    log.debug("[数据加载] 批量写入进度: {}/{} for doc={}", end, vectorDocs.size(), docId);
                    success = true;
                    break;
                } catch (Exception e) {
                    lastException = e;
                    log.warn("[数据加载] 向量批量写入失败 (尝试 {}/{}): docId={}, batchIndex={}, error={}",
                            attempt, MAX_VECTOR_WRITE_RETRIES, docId, i, e.getMessage());
                    if (attempt < MAX_VECTOR_WRITE_RETRIES) {
                        try {
                            Thread.sleep(1000L * attempt); // 指数退避
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }

            if (!success) {
                log.error("[数据加载] 向量批量写入最终失败: docId={}, batchIndex={}, 已重试{}次",
                        docId, i, MAX_VECTOR_WRITE_RETRIES);
                throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        "向量库写入异常（已重试" + MAX_VECTOR_WRITE_RETRIES + "次）: " +
                                (lastException != null ? lastException.getMessage() : "未知错误"));
            }
        }
        log.info("[数据加载] 向量加载完成，共 {} 条: doc={}", vectorDocs.size(), docId);
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

    private List<Document> applyKeywordMetadata(List<Document> chunks, Long docId) {
        String mode = Optional.ofNullable(knowledgeProperties.getKeywordMetadataMode()).orElse("rule").toLowerCase(Locale.ROOT);
        int n = Math.max(1, knowledgeProperties.getKeywordMetadataCount());

        // 优化：优先使用 LLM 模式提取关键词，提升检索质量
        if ("llm".equals(mode) && knowledgeProperties.isKeywordMetadataEnrichEnabled()) {
            try {
                log.info("[关键词增强] 使用 KeywordMetadataEnricher (LLM): doc={}, chunkCount={}, keywordCount={}",
                        docId, chunks.size(), n);
                KeywordMetadataEnricher enricher = KeywordMetadataEnricher.builder(chatModel)
                        .keywordCount(n)
                        .build();
                List<Document> llmDocs = enricher.apply(chunks);
                log.info("[关键词增强] LLM 增强完成: doc={}", docId);
                return llmDocs;
            } catch (Exception ex) {
                log.error("[关键词增强] KeywordMetadataEnricher 失败，回退规则关键词: doc={}, err={}", docId, ex.getMessage(), ex);
                // 记录失败指标
                io.micrometer.core.instrument.Counter.builder(RagMetricEnum.KEYWORD_EXTRACTION_FAILURE_COUNT.getValue())
                        .tag("mode", "llm")
                        .register(meterRegistry)
                        .increment();
            }
        }

        // 降级：使用规则提取
        log.info("[关键词增强] 使用规则提取: doc={}, chunkCount={}", docId, chunks.size());
        return applyRuleKeywords(chunks, n);
    }

    private List<Document> applyRuleKeywords(List<Document> chunks, int limit) {
        List<Document> out = new ArrayList<>(chunks.size());
        for (Document d : chunks) {
            String kw = extractRuleKeywords(d.getText(), limit);
            Map<String, Object> metadata = d.getMetadata() != null ? new HashMap<>(d.getMetadata()) : new HashMap<>();
            metadata.put("excerpt_keywords", kw);
            out.add(new Document(d.getId(), d.getText(), metadata));
        }
        return out;
    }

    private String extractRuleKeywords(String text, int limit) {
        if (text == null || text.isBlank()) {
            return "";
        }
        LinkedHashSet<String> kws = new LinkedHashSet<>();
        String normalized = text.replaceAll("[^\\p{IsHan}\\p{Alnum}\\s]", " ");
        for (String token : normalized.split("\\s+")) {
            if (token.length() < 2) {
                continue;
            }
            String t = token.toLowerCase(Locale.ROOT);
            if (Set.of("the", "and", "for", "with", "this", "that", "from", "http", "https").contains(t)) {
                continue;
            }
            kws.add(token);
            if (kws.size() >= limit) {
                break;
            }
        }
        return String.join(", ", kws);
    }

    private int estimateToken(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int cjk = 0;
        int nonCjk = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                cjk++;
            } else {
                nonCjk++;
            }
        }
        return cjk + (int) Math.ceil(nonCjk / 4.0);
    }

    private void updateDocumentStatus(KnowledgeDocument doc, KnowledgeParseStatusEnum status, String error) {
        doc.setParseStatus(status.getValue());
        if (error != null) doc.setErrorMsg(error.length() > 2000 ? error.substring(0, 2000) : error);
        knowledgeDocumentService.updateById(doc);
    }

    @Override
    public void deleteVectors(Long documentId) {
        if (documentId == null) {
            log.warn("[向量删除] 文档ID为空，跳过删除");
            return;
        }

        try {
            knowledgeVectorStore.delete(filterParser.parse("documentId == '" + documentId + "'"));
            log.info("[向量删除] 成功删除文档关联向量: docId={}", documentId);
        } catch (Exception e) {
            log.error("[向量删除] 删除向量失败: docId={}, error={}", documentId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "向量库删除失败: " + e.getMessage());
        }
    }
}
