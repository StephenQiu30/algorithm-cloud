package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.config.RagRetrievalProperties;
import com.stephen.cloud.ai.convert.DocumentChunkConvert;
import com.stephen.cloud.ai.knowledge.retrieval.RRFFusionService;
import com.stephen.cloud.ai.mapper.DocumentChunkMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.service.ChunkService;
import com.stephen.cloud.ai.service.KeywordSearchService;
import com.stephen.cloud.ai.service.VectorStoreService;
import com.stephen.cloud.api.ai.model.dto.chunk.ChunkQueryRequest;
import com.stephen.cloud.api.ai.model.dto.chunk.ChunkSearchRequest;
import com.stephen.cloud.api.ai.model.vo.ChunkVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ChunkServiceImpl extends ServiceImpl<DocumentChunkMapper, DocumentChunk> implements ChunkService {

    @Resource
    private VectorStoreService vectorStoreService;

    @Resource
    private KeywordSearchService keywordSearchService;

    @Resource
    private RRFFusionService rrfFusionService;

    @Resource
    private RagRetrievalProperties ragRetrievalProperties;

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
    public Page<ChunkVO> getChunkVOPage(Page<DocumentChunk> page) {
        List<DocumentChunk> records = page.getRecords();
        Page<ChunkVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isEmpty(records)) {
            return voPage;
        }
        List<ChunkVO> voList = records.stream()
                .map(DocumentChunkConvert.INSTANCE::objToVo)
                .toList();
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public ChunkVO getChunkVO(DocumentChunk chunk) {
        return DocumentChunkConvert.INSTANCE.objToVo(chunk);
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
        List<Document> vectorDocs;
        if (request.getDocumentId() != null && request.getDocumentId() > 0) {
            vectorDocs = vectorStoreService.searchByDocumentId(query, request.getDocumentId(), vectorTopK, request.getSimilarityThreshold());
        } else {
            vectorDocs = vectorStoreService.similaritySearch(query, request.getKnowledgeBaseId(), vectorTopK, request.getSimilarityThreshold());
        }

        // 关键词检索
        Map<String, String> metadataFilters = Map.of();
        Long keywordKbId = request.getKnowledgeBaseId();
        List<Document> keywordDocs = keywordSearchService.bm25Search(query, keywordKbId, keywordTopK, metadataFilters);

        // 如果指定了 documentId，过滤关键词结果
        if (request.getDocumentId() != null && request.getDocumentId() > 0) {
            String targetDocId = String.valueOf(request.getDocumentId());
            keywordDocs = keywordDocs.stream()
                    .filter(doc -> targetDocId.equals(String.valueOf(doc.getMetadata().get("documentId"))))
                    .toList();
        }

        // RRF 融合
        List<Document> fusedDocs = rrfFusionService.fuse(vectorDocs, keywordDocs, finalTopK, rrfK);

        log.info("[ChunkSearch] query='{}', vectorHits={}, keywordHits={}, fusedTopK={}",
                query, vectorDocs.size(), keywordDocs.size(), fusedDocs.size());

        // 转换为 ChunkVO
        List<ChunkVO> result = new ArrayList<>();
        for (Document doc : fusedDocs) {
            ChunkVO vo = new ChunkVO();
            Object documentId = doc.getMetadata().get("documentId");
            Object documentName = doc.getMetadata().get("documentName");
            Object chunkIndex = doc.getMetadata().get("chunkIndex");
            Object score = doc.getMetadata().get("fusionScore");
            Object knowledgeBaseId = doc.getMetadata().get("knowledgeBaseId");

            vo.setId(doc.getId());
            if (documentId != null) {
                vo.setDocumentId(Long.valueOf(String.valueOf(documentId)));
            }
            vo.setDocumentName(documentName == null ? null : String.valueOf(documentName));
            if (chunkIndex != null) {
                vo.setChunkIndex(Integer.valueOf(String.valueOf(chunkIndex)));
            }
            if (knowledgeBaseId != null) {
                vo.setKnowledgeBaseId(Long.valueOf(String.valueOf(knowledgeBaseId)));
            }
            vo.setContent(doc.getText());
            vo.setWordCount(doc.getText() == null ? 0 : doc.getText().length());
            if (score != null) {
                vo.setScore(Double.valueOf(String.valueOf(score)));
            }
            vo.setSourceType(doc.getMetadata().get("sourceType") == null ? null : String.valueOf(doc.getMetadata().get("sourceType")));
            vo.setMatchReason(doc.getMetadata().get("matchReason") == null ? null : String.valueOf(doc.getMetadata().get("matchReason")));
            result.add(vo);
        }
        return result;
    }
}
