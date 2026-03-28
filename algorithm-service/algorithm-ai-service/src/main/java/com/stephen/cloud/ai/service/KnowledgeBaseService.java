package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.api.ai.model.dto.knowledgebase.KnowledgeBaseQueryRequest;
import com.stephen.cloud.api.ai.model.vo.KnowledgeBaseVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 知识库管理服务接口
 * <p>
 * 提供知识库的增删改查、数据校验、视图转换等能力
 * </p>
 *
 * @author StephenQiu30
 */
public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    /**
     * 校验知识库数据合法性
     *
     * @param knowledgeBase 待校验的知识库对象
     * @param add           是否为新增操作
     */
    void validKnowledgeBase(KnowledgeBase knowledgeBase, boolean add);

    /**
     * 构建知识库查询条件封装
     *
     * @param queryRequest 查询请求对象
     * @return 查询条件封装
     */
    LambdaQueryWrapper<KnowledgeBase> getQueryWrapper(KnowledgeBaseQueryRequest queryRequest);

    /**
     * 获取知识库视图对象
     *
     * @param knowledgeBase 知识库实体
     * @param request       HTTP 请求
     * @return 脱敏后的知识库视图
     */
    KnowledgeBaseVO getKnowledgeBaseVO(KnowledgeBase knowledgeBase, HttpServletRequest request);

    /**
     * 分页获取知识库视图
     *
     * @param page    知识库分页数据
     * @param request HTTP 请求
     * @return 知识库视图分页对象
     */
    Page<KnowledgeBaseVO> getKnowledgeBaseVOPage(Page<KnowledgeBase> page, HttpServletRequest request);

    /**
     * 检查知识库名称是否唯一
     *
     * @param name      知识库名称
     * @param excludeId 排除的知识库 ID（用于更新时排除自身）
     * @return true 表示名称唯一
     */
    boolean isNameUnique(String name, Long excludeId);

    /**
     * 删除知识库
     *
     * @param id          知识库 ID
     * @param loginUserId 当前登录用户 ID
     * @param isAdmin     当前登录用户是否为管理员
     * @return true 表示删除成功
     */
    boolean deleteKnowledgeBaseById(Long id, Long loginUserId, boolean isAdmin);
}
