package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.config.RagRetrievalProperties;
import com.stephen.cloud.ai.convert.DocumentChunkConvert;
import com.stephen.cloud.ai.knowledge.retrieval.RagDocumentHelper;
import com.stephen.cloud.ai.knowledge.retrieval.RRFFusionService;
import com.stephen.cloud.ai.mapper.DocumentChunkMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.service.ChunkService;
import com.stephen.cloud.ai.service.KeywordSearchService;
import com.stephen.cloud.ai.service.VectorStoreService;
import com.stephen.cloud.api.ai.model.dto.chunk.ChunkQueryRequest;
import com.stephen.cloud.api.ai.model.dto.chunk.ChunkSearchRequest;
import com.stephen.cloud.api.ai.model.vo.ChunkVO;
import com.stephen.cloud.api.search.model.entity.ChunkEsDTO;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncDataTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.EsSyncBatchMessage;
import com.stephen.cloud.common.rabbitmq.model.EsSyncMessage;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.*;


import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChunkServiceImpl extends ServiceImpl<DocumentChunkMapper, DocumentChunk> implements ChunkService {

    @Resource
    private VectorStoreService vectorStoreService;

    @Resource
    private KeywordSearchService keywordSearchService;

    @Resource
    private RabbitMqSender mqSender;

    @Resource
    private RRFFusionService rrfFusionService;

    @Resource
    private RagRetrievalProperties ragRetrievalProperties;

    @Resource
    private RagDocumentHelper ragDocumentHelper;

    @Override
    public LambdaQueryWrapper<DocumentChunk> getQueryWrapper(ChunkQueryRequest queryRequest) {
        LambdaQueryWrapper<DocumentChunk> queryWrapper = new LambdaQueryWrapper<>();
        if (queryRequest == null) {
            return queryWrapper;
        }
        queryWrapper.eq(queryRequest.getDocumentId() != null && queryRequest.getDocumentId() > 0,
                        DocumentChunk::getDocumentId, queryRequest.getDocumentId())
                .eq(queryRequest.getKnowledgeBaseId() != null && queryRequest.getKnowledgeBaseId() > 0,
                        DocumentChunk::getKnowledgeBaseId, queryRequest.getKnowledgeBaseId())
                .orderByAsc(DocumentChunk::getChunkIndex);
        return queryWrapper;
    }

    @Override
    public Page<ChunkVO> getChunkVOPage(Page<DocumentChunk> page, HttpServletRequest request) {
        List<DocumentChunk> records = page.getRecords();
        Page<ChunkVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isEmpty(records)) {
            return voPage;
        }
        List<ChunkVO> voList = records.stream()
                .map(chunk -> this.getChunkVO(chunk, request))
                .toList();
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public ChunkVO getChunkVO(DocumentChunk chunk, HttpServletRequest request) {
        return DocumentChunkConvert.objToVo(chunk);
    }

    @Override
    public List<ChunkVO> searchChunks(ChunkSearchRequest request) {
        String query = request.getQuery();
        if (StringUtils.isBlank(query)) {
            return List.of();
        }
        int finalTopK = request.getTopK() == null || request.getTopK() <= 0
                ? ragRetrievalProperties.getTopK() : request.getTopK();
        int vectorTopK = ragRetrievalProperties.getVectorTopK() <= 0 ? finalTopK : ragRetrievalProperties.getVectorTopK();
        int keywordTopK = ragRetrievalProperties.getKeywordTopK() <= 0 ? finalTopK : ragRetrievalProperties.getKeywordTopK();
        int rrfK = ragRetrievalProperties.getRrfK() <= 0 ? 60 : ragRetrievalProperties.getRrfK();

        // 向量检索
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op op = null;
        if (request.getKnowledgeBaseId() != null && request.getKnowledgeBaseId() > 0) {
            op = b.eq(KNOWLEDGE_BASE_ID, request.getKnowledgeBaseId());
        }
        if (request.getDocumentId() != null && request.getDocumentId() > 0) {
            FilterExpressionBuilder.Op docFilter = b.eq(DOCUMENT_ID, request.getDocumentId());
            op = (op == null) ? docFilter : b.and(op, docFilter);
        }
        Filter.Expression filterExpression = (op == null) ? null : op.build();

        List<Document> vectorDocs;
        if (request.getDocumentId() != null && request.getDocumentId() > 0) {
            vectorDocs = vectorStoreService.searchByDocumentId(query, request.getDocumentId(), vectorTopK, request.getSimilarityThreshold());
        } else {
            vectorDocs = vectorStoreService.similaritySearch(query, filterExpression, vectorTopK, request.getSimilarityThreshold());
        }

        // 关键词检索
        List<Document> keywordDocs = keywordSearchService.bm25Search(query, keywordTopK, filterExpression);

        // 加权 RRF 融合
        List<Document> fusedDocs = rrfFusionService.fuse(vectorDocs, keywordDocs, finalTopK, rrfK,
                ragRetrievalProperties.getVectorWeight(), ragRetrievalProperties.getKeywordWeight());

        log.info("[ChunkSearch] query='{}', vectorHits={}, keywordHits={}, fusedTopK={}",
                query, vectorDocs.size(), keywordDocs.size(), fusedDocs.size());

        // 转换为 ChunkVO
        return ragDocumentHelper.toChunkVOs(fusedDocs);
    }

    /**
     * 同步单个分片到 ES
     *
     * @param chunkId 分片 ID
     */
    @Override
    public void syncToEs(Long chunkId) {
        if (chunkId == null || chunkId <= 0) {
            return;
        }
        DocumentChunk chunk = this.getById(chunkId);
        if (chunk == null) {
            log.info("[DocumentChunkServiceImpl] 分片不存在或已被逻辑删除，从 ES 中删除: id={}", chunkId);
            EsSyncMessage message = new EsSyncMessage(
                    EsSyncDataTypeEnum.CHUNK.getValue(), "delete", chunkId, null, System.currentTimeMillis());
            mqSender.send(MqBizTypeEnum.ES_SYNC_SINGLE, chunkId + ":" + System.currentTimeMillis(), message);
            return;
        }
        try {
            ChunkEsDTO chunkEsDTO = DocumentChunkConvert.objToEsDTO(chunk);
            EsSyncMessage message = new EsSyncMessage(
                    EsSyncDataTypeEnum.CHUNK.getValue(), "upsert", chunkId, JSONUtil.toJsonStr(chunkEsDTO),
                    System.currentTimeMillis());
            mqSender.send(MqBizTypeEnum.ES_SYNC_SINGLE, chunkId + ":" + System.currentTimeMillis(), message);
            log.info("[DocumentChunkServiceImpl] 单个分片 ES 同步消息已发送, chunkId: {}", chunkId);
        } catch (Exception e) {
            log.error("【ES同步失败】单个分片 ES 同步消息发送失败, chunkId: {}", chunkId, e);
        }
    }

    /**
     * 同步分片数据到 ES (全量或增量)
     */
    @Override
    public void syncToEs(EsSyncTypeEnum syncType, Date minUpdateTime) {
        log.info("[DocumentChunkServiceImpl] 开始同步分片数据到 ES, 方式: {}, 起始时间: {}", syncType, minUpdateTime);

        long pageSize = 1000; // 分片数据量大，使用较大的 BatchSize
        Long lastId = 0L;

        while (true) {
            LambdaQueryWrapper<DocumentChunk> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.gt(DocumentChunk::getId, lastId);
            queryWrapper.ge(minUpdateTime != null, DocumentChunk::getUpdateTime, minUpdateTime);
            queryWrapper.orderByAsc(DocumentChunk::getId);
            queryWrapper.last("limit " + pageSize);

            List<DocumentChunk> chunkList = this.list(queryWrapper);
            if (CollUtil.isEmpty(chunkList)) {
                break;
            }

            List<ChunkEsDTO> esDTOList = chunkList.stream()
                    .map(DocumentChunkConvert::objToEsDTO)
                    .toList();

            EsSyncBatchMessage batchMessage = new EsSyncBatchMessage();
            batchMessage.setDataType(EsSyncDataTypeEnum.CHUNK.getValue());
            batchMessage.setOperation("upsert");
            batchMessage.setDataContentList(esDTOList.stream().map(JSONUtil::toJsonStr)
                    .collect(Collectors.toList()));
            batchMessage.setTimestamp(System.currentTimeMillis());

            mqSender.send(MqBizTypeEnum.ES_SYNC_BATCH, batchMessage);

            log.info("[DocumentChunkServiceImpl] 已发送 {} 条分片同步消息, lastId: {}", esDTOList.size(), lastId);

            if (chunkList.size() < pageSize) {
                break;
            }
            lastId = chunkList.get(chunkList.size() - 1).getId();
        }
        log.info("[DocumentChunkServiceImpl] 分片数据同步指令处理完成");
    }
}
