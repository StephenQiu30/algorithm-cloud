package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.api.ai.model.dto.knowledgebase.KnowledgeBaseQueryRequest;
import com.stephen.cloud.api.ai.model.vo.KnowledgeBaseVO;
import jakarta.servlet.http.HttpServletRequest;

public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    void validKnowledgeBase(KnowledgeBase knowledgeBase, boolean add);

    LambdaQueryWrapper<KnowledgeBase> getQueryWrapper(KnowledgeBaseQueryRequest queryRequest);

    KnowledgeBaseVO getKnowledgeBaseVO(KnowledgeBase knowledgeBase, HttpServletRequest request);

    Page<KnowledgeBaseVO> getKnowledgeBaseVOPage(Page<KnowledgeBase> page, HttpServletRequest request);

    boolean isNameUnique(String name, Long excludeId);
}
