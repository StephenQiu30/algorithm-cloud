package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.KnowledgeDocument;
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
     * 获取指定文档并校验操作权限
     *
     * @param knowledgeBaseId 知识库 ID
     * @param documentId      文档 ID
     * @param userId          用户 ID
     * @return 文档实体
     */
    KnowledgeDocument getDocumentForUser(Long knowledgeBaseId, Long documentId, Long userId);
}
