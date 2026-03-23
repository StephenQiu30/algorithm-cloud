package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.KnowledgeDocument;
import com.stephen.cloud.api.knowledge.model.dto.knowledgedocument.KnowledgeDocumentQueryRequest;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeDocumentVO;

/**
 * 知识文档服务接口
 *
 * @author StephenQiu30
 */
public interface KnowledgeDocumentService extends IService<KnowledgeDocument> {

    /**
     * 校验文档合法性
     */
    void validKnowledgeDocument(KnowledgeDocument entity, boolean add);

    /**
     * 获取查询条件
     */
    LambdaQueryWrapper<KnowledgeDocument> getQueryWrapper(KnowledgeDocumentQueryRequest queryRequest);

    /**
     * 获取文档视图
     */
    KnowledgeDocumentVO getKnowledgeDocumentVO(KnowledgeDocument entity);

    /**
     * 获取文档视图分页
     */
    Page<KnowledgeDocumentVO> getKnowledgeDocumentVOPage(Page<KnowledgeDocument> page);
}
