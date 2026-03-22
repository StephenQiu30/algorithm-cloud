package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.api.knowledge.model.dto.KnowledgeBaseQueryRequest;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeBaseVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 知识库服务
 * <p>
 * 提供知识库的增删改查、权限校验及针对不同请求场景的分页封装能力。
 * </p>
 *
 * @author StephenQiu30
 */
public interface KnowledgeService extends IService<KnowledgeBase> {

    /**
     * 校验知识库信息
     *
     * @param knowledgeBase 知识库实体
     * @param add           是否为新增操作
     */
    void validKnowledgeBase(KnowledgeBase knowledgeBase, boolean add);

    /**
     * 根据查询请求构建 MyBatis Plus 的查询条件封装
     *
     * @param knowledgeBaseQueryRequest 知识库查询请求对象
     * @return LambdaQueryWrapper 查询条件封装
     */
    LambdaQueryWrapper<KnowledgeBase> getQueryWrapper(KnowledgeBaseQueryRequest knowledgeBaseQueryRequest);

    /**
     * 获取知识库封装 (脱敏及关联信息填充)
     *
     * @param knowledgeBase 知识库实体
     * @param request       HTTP 请求
     * @return 知识库视图
     */
    KnowledgeBaseVO getKnowledgeBaseVO(KnowledgeBase knowledgeBase, HttpServletRequest request);

    /**
     * 分页获取知识库封装
     *
     * @param knowledgeBasePage 知识库分页实体
     * @param request           HTTP 请求
     * @return 知识库视图分页
     */
    Page<KnowledgeBaseVO> getKnowledgeBaseVOPage(Page<KnowledgeBase> knowledgeBasePage, HttpServletRequest request);
}
