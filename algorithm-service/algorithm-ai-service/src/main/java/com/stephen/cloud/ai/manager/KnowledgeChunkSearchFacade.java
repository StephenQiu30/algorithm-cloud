package com.stephen.cloud.ai.manager;

import cn.hutool.core.convert.Convert;
import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.service.KnowledgeBaseService;
import com.stephen.cloud.ai.service.VectorStoreService;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Component;

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
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private KnowledgeProperties knowledgeProperties;

    /**
     * 校验知识库存在后在指定知识库内语义检索。
     */
    public List<ChunkSourceVO> searchChunks(Long knowledgeBaseId, String query, Integer requestTopK,
            int topKMax) {
        ThrowUtils.throwIf(knowledgeBaseId == null || knowledgeBaseId <= 0, ErrorCode.PARAMS_ERROR, "知识库 ID 不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(query), ErrorCode.PARAMS_ERROR, "查询内容不能为空");
        ThrowUtils.throwIf(knowledgeBaseService.getById(knowledgeBaseId) == null, ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        return executeChunkSearch(knowledgeBaseId, query.trim(), requestTopK, topKMax);
    }

    /**
     * 调用方已确认知识库存在时检索（避免重复 {@code getById}），如 RAG 在加载 {@code KnowledgeBase} 后调用。
     */
    public List<ChunkSourceVO> searchChunksForVerifiedKnowledgeBase(Long knowledgeBaseId, String query,
            Integer requestTopK, int topKMax) {
        ThrowUtils.throwIf(knowledgeBaseId == null || knowledgeBaseId <= 0, ErrorCode.PARAMS_ERROR, "知识库 ID 不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(query), ErrorCode.PARAMS_ERROR, "查询内容不能为空");
        return executeChunkSearch(knowledgeBaseId, query.trim(), requestTopK, topKMax);
    }

    private List<ChunkSourceVO> executeChunkSearch(Long knowledgeBaseId, String queryTrimmed,
            Integer requestTopK, int topKMax) {
        // 1. 获取 TopK 且保证不超过上限（结合 Java 21 特性简化逻辑）
        int topK = Math.min(
                (requestTopK != null && requestTopK > 0) ? requestTopK : knowledgeProperties.getDefaultTopK(),
                topKMax
        );

        // 2. 构造检索请求（元数据过滤条件与 Post 服务 ES 同步逻辑一致）
        SearchRequest searchRequest = SearchRequest.builder()
                .query(queryTrimmed)
                .topK(topK)
                .similarityThreshold(knowledgeProperties.getSimilarityThreshold())
                .filterExpression("knowledgeBaseId == '" + knowledgeBaseId + "'")
                .build();

        // 3. 执行语义检索并使用 Stream 流式处理结果输出
        return vectorStoreService.similaritySearch(searchRequest).stream()
                .map(d -> {
                    Long chunkId = Convert.toLong(d.getMetadata().get("chunkId"));
                    Long documentId = Convert.toLong(d.getMetadata().get("documentId"));
                    String documentName = Convert.toStr(d.getMetadata().get("documentName"));

                    return ChunkSourceVO.builder()
                            .chunkId(chunkId)
                            .documentId(documentId)
                            .documentName(documentName)
                            .content(d.getText())
                            .score(d.getScore())
                            .build();
                }).toList();
    }
}
