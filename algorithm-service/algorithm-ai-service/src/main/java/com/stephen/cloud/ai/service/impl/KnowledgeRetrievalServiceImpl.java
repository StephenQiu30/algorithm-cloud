package com.stephen.cloud.ai.service.impl;

import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.manager.KnowledgeChunkSearchFacade;
import com.stephen.cloud.ai.service.KnowledgeRetrievalService;
import com.stephen.cloud.api.knowledge.model.dto.retrieval.KnowledgeRetrievalRequest;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

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
    private KnowledgeChunkSearchFacade knowledgeChunkSearchFacade;

    @Resource
    private KnowledgeProperties knowledgeProperties;

    /**
     * 执行知识库语义检索
     * <p>
     * 核心步骤：
     * 1. 验证查询内容不为空且知识库 ID 有效。
     * 2. 调用检索门面 (Facade)，由门面根据系统配置决定执行单路 kNN 检索还是 Hybrid 混合检索。
     * 3. 结果集包含相似度分数 (score) 及元数据生成的溯源信息。
     * </p>
     *
     * @param request 包含查询文本、目标知识库及可选 topK 的请求对象
     * @param userId  当前操作用户 ID
     * @return 命中的分片结果列表，按相似度降序排列
     * @throws BusinessException 当参数校验失败时抛出
     */
    @Override
    public List<ChunkSourceVO> search(KnowledgeRetrievalRequest request, Long userId) {
        if (request == null || StringUtils.isBlank(request.getQuery())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "检索内容不能为空");
        }
        Long kbId = request.getKnowledgeBaseId();
        if (kbId == null || kbId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标知识库 ID 不能为空");
        }

        // 调用检索门面执行混合搜索或单路向量搜索
        return knowledgeChunkSearchFacade.searchChunks(
                kbId,
                request.getQuery().trim(),
                request.getTopK(),
                knowledgeProperties.getRetrievalTopKMax());
    }
}
