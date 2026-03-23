package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.KnowledgeDocument;
import com.stephen.cloud.api.knowledge.model.dto.KnowledgeDocumentQueryRequest;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeDocumentVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识文档服务
 * <p>
 * 负责知识库文档资源的上传、权限核对及文档元数据管理。
 * </p>
 *
 * @author StephenQiu30
 */
public interface KnowledgeDocumentService extends IService<KnowledgeDocument> {

    /**
     * 上传知识文档并触发异步解析流程
     *
     * @param knowledgeBaseId 目标知识库 ID
     * @param file            上传的文件
     * @param userId          执行操作的用户 ID
     * @return 数据库文档 ID
     */
    Long uploadDocument(Long knowledgeBaseId, MultipartFile file, Long userId);

    /**
     * 删除文档并同步清除向量库中的切片
     *
     * @param documentId 文档 ID
     * @param userId     用户 ID
     * @return 是否成功
     */
    boolean deleteDocument(Long documentId, Long userId);

    /**
     * 获取指定文档并校验操作权限
     *
     * @param knowledgeBaseId 知识库 ID
     * @param documentId      文档 ID
     * @param userId          用户 ID
     * @return 文档实体
     */
    KnowledgeDocument getDocumentForUser(Long knowledgeBaseId, Long documentId, Long userId);

    /**
     * 获取查询条件
     *
     * @param queryRequest 查询请求
     * @return 查询条件包装类
     */
    QueryWrapper<KnowledgeDocument> getQueryWrapper(KnowledgeDocumentQueryRequest queryRequest);

    /**
     * 分页映射 VO
     *
     * @param documentPage 原始分页数据
     * @return 增强后的 VO 分页数据
     */
    Page<KnowledgeDocumentVO> getKnowledgeDocumentVOPage(Page<KnowledgeDocument> documentPage);
}
