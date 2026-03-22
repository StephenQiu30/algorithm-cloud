package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.convert.Convert;
import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.service.KnowledgeRetrievalService;
import com.stephen.cloud.ai.service.KnowledgeService;
import com.stephen.cloud.ai.service.VectorStoreService;
import com.stephen.cloud.api.knowledge.model.dto.KnowledgeRetrievalRequest;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识检索服务实现
 * <p>
 * 对接向量数据库执行语义搜索，并支持基于 Metadata 的多租户（知识库隔离）过滤。
 * </p>
 *
 * @author StephenQiu30
 */
@Service
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    @Resource
    private VectorStoreService vectorStoreService;

    @Resource
    private KnowledgeService knowledgeService;

    @Resource
    private KnowledgeProperties knowledgeProperties;

    /**
     * 执行语义检索
     *
     * @param request 检索请求（包含 query, kbId, topK 等）
     * @param userId  执行用户 ID（用于鉴权）
     * @return 命中的分片列表封装
     */
    @Override
    public List<ChunkSourceVO> search(KnowledgeRetrievalRequest request, Long userId) {
        if (request == null || StringUtils.isBlank(request.getQuery())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询内容不能为空");
        }
        Long kbId = request.getKnowledgeBaseId();
        if (kbId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库 ID 不能为空");
        }

        // 1. 权限检查
        knowledgeService.getAndCheckAccess(kbId, userId);

        // 2. 准备检索参数
        int topK = request.getTopK() != null && request.getTopK() > 0
                ? request.getTopK()
                : knowledgeProperties.getDefaultTopK();
        
        // 构造 Metadata 过滤
        String filterExpression = "knowledgeBaseId == '" + kbId + "'";
        
        SearchRequest searchRequest = SearchRequest.builder()
                .query(request.getQuery().trim())
                .topK(Math.min(topK, 50)) // 限制测试检索上限
                .similarityThreshold(0.0) 
                .filterExpression(filterExpression)
                .build();

        // 3. 执行检索
        List<Document> docHits = vectorStoreService.similaritySearch(searchRequest);
        
        // 4. 封装结果
        List<ChunkSourceVO> results = new ArrayList<>();
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
