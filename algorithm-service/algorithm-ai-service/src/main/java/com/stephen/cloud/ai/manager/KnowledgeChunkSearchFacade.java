package com.stephen.cloud.ai.manager;

import cn.hutool.core.convert.Convert;
import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.service.KnowledgeService;
import com.stephen.cloud.ai.service.VectorStoreService;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 按知识库维度的向量检索与分片结果封装门面。
 *
 * @author StephenQiu30
 */
@Component
public class KnowledgeChunkSearchFacade {

    @Resource
    private VectorStoreService vectorStoreService;

    @Resource
    private KnowledgeService knowledgeService;

    @Resource
    private KnowledgeProperties knowledgeProperties;

    /**
     * 校验知识库存在后在指定知识库内语义检索。
     */
    public List<ChunkSourceVO> searchChunks(Long knowledgeBaseId, String query, Integer requestTopK,
            int topKMax) {
        assertKnowledgeBaseId(knowledgeBaseId);
        validateQueryText(query);
        if (knowledgeService.getById(knowledgeBaseId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        }
        return executeChunkSearch(knowledgeBaseId, query.trim(), requestTopK, topKMax);
    }

    /**
     * 调用方已确认知识库存在时检索（避免重复 {@code getById}），如 RAG 在加载 {@code KnowledgeBase} 后调用。
     */
    public List<ChunkSourceVO> searchChunksForVerifiedKnowledgeBase(Long knowledgeBaseId, String query,
            Integer requestTopK, int topKMax) {
        assertKnowledgeBaseId(knowledgeBaseId);
        validateQueryText(query);
        return executeChunkSearch(knowledgeBaseId, query.trim(), requestTopK, topKMax);
    }

    private static void assertKnowledgeBaseId(Long knowledgeBaseId) {
        if (knowledgeBaseId == null || knowledgeBaseId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库 ID 不能为空");
        }
    }

    private static void validateQueryText(String query) {
        if (StringUtils.isBlank(query)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询内容不能为空");
        }
    }

    private List<ChunkSourceVO> executeChunkSearch(long knowledgeBaseId, String queryTrimmed,
            Integer requestTopK, int topKMax) {
        int topK = requestTopK != null && requestTopK > 0
                ? requestTopK
                : knowledgeProperties.getDefaultTopK();
        topK = Math.min(topK, topKMax);

        String filterExpression = "knowledgeBaseId == '" + knowledgeBaseId + "'";
        SearchRequest searchRequest = SearchRequest.builder()
                .query(queryTrimmed)
                .topK(topK)
                .similarityThreshold(knowledgeProperties.getSimilarityThreshold())
                .filterExpression(filterExpression)
                .build();

        List<Document> docHits = vectorStoreService.similaritySearch(searchRequest);
        List<ChunkSourceVO> results = new ArrayList<>(docHits.size());
        for (Document d : docHits) {
            Long chunkId = Convert.toLong(d.getMetadata().get("chunkId"));
            double score = d.getScore() != null ? d.getScore() : 0.0;
            results.add(ChunkSourceVO.builder()
                    .chunkId(chunkId)
                    .content(d.getText())
                    .score(score)
                    .build());
        }
        return results;
    }
}
