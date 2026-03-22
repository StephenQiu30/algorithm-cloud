package com.stephen.cloud.ai.service.impl;

import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.manager.KnowledgeChunkSearchFacade;
import com.stephen.cloud.ai.service.KnowledgeRetrievalService;
import com.stephen.cloud.api.knowledge.model.dto.KnowledgeRetrievalRequest;
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

    @Override
    public List<ChunkSourceVO> search(KnowledgeRetrievalRequest request, Long userId) {
        if (request == null || StringUtils.isBlank(request.getQuery())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询内容不能为空");
        }
        Long kbId = request.getKnowledgeBaseId();
        if (kbId == null || kbId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库 ID 不能为空");
        }

        return knowledgeChunkSearchFacade.searchChunks(
                kbId,
                request.getQuery().trim(),
                request.getTopK(),
                knowledgeProperties.getRetrievalTopKMax());
    }
}
