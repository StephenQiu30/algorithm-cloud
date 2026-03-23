package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.api.knowledge.model.dto.knowledgebase.KnowledgeBaseQueryRequest;
import com.stephen.cloud.api.knowledge.model.dto.retrieval.KnowledgeRetrievalRequest;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeBaseVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 统一知识库业务管理接口：负责知识库核心生命周期。
 *
 * @author StephenQiu30
 */
public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    // ========================================================================
    // 1. 知识库 CRUD (Core)
    // ========================================================================

    /**
     * 校验知识库参数合法性
     *
     * @param knowledgeBase 知识库对象
     * @param add           是否为新增操作
     */
    void validKnowledgeBase(KnowledgeBase knowledgeBase, boolean add);

    /**
     * 构建查询条件包装器
     *
     * @param queryRequest 查询请求对象
     * @return LambdaQueryWrapper
     */
    LambdaQueryWrapper<KnowledgeBase> getQueryWrapper(KnowledgeBaseQueryRequest queryRequest);

    /**
     * 获取知识库视图对象封装
     *
     * @param knowledgeBase 知识库实体对象
     * @param request       HTTP请求对象
     * @return KnowledgeBaseVO
     */
    KnowledgeBaseVO getKnowledgeBaseVO(KnowledgeBase knowledgeBase, HttpServletRequest request);

    /**
     * 获取知识库分页视图对象列表
     *
     * @param page    分页对象
     * @param request HTTP请求对象
     * @return Page<KnowledgeBaseVO>
     */
    Page<KnowledgeBaseVO> getKnowledgeBaseVOPage(Page<KnowledgeBase> page, HttpServletRequest request);

    // ========================================================================
    // 2. 知识库业务功能 (Business Orchestration)
    // ========================================================================

    /**
     * 上传文档并触发存储与异步解析
     *
     * @param knowledgeBaseId 知识库 ID
     * @param file            上传的文件对象
     * @param userId          用户 ID
     * @return Long 文档 ID
     */
    Long uploadDocument(Long knowledgeBaseId, MultipartFile file, Long userId);

    /**
     * 联级清理：从数据库中删除文档、分片记录，并从向量库中删除关联向量
     *
     * @param documentId 文档 ID
     * @param userId     用户 ID
     * @return boolean
     */
    boolean deleteDocumentAndAssociated(Long documentId, Long userId);

    // ========================================================================
    // 3. 语义检索功能 (Retrieval)
    // ========================================================================

    /**
     * 面向业务的封装检索：校验权限、查询、并映射为 VO
     *
     * @param request 检索请求对象
     * @param userId  用户 ID
     * @return List<ChunkSourceVO>
     */
    List<ChunkSourceVO> searchChunks(KnowledgeRetrievalRequest request, Long userId);

    /**
     * 底层向量相似度检索入口
     *
     * @param searchRequest 向量搜索请求对象
     * @return List<Document>
     */
    List<Document> similaritySearch(SearchRequest searchRequest);
}
