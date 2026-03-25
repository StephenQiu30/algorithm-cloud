package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.Document;
import com.stephen.cloud.api.ai.model.dto.document.DocumentQueryRequest;
import com.stephen.cloud.api.ai.model.vo.DocumentVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档管理服务接口
 * <p>
 * 提供知识库文档的上传、删除、数据校验、视图转换等能力。
 * 文档上传后将自动触发 ETL 处理流程，完成解析、分片、向量化并同步至向量数据库。
 * </p>
 *
 * @author StephenQiu30
 */
public interface DocumentService extends IService<Document> {

    /**
     * 上传文档并触发异步处理
     * <p>
     * 文档将先保存至存储服务，再发送处理消息至 MQ 队列，触发完整的 ETL 处理流程。
     *
     * @param file             上传的文件
     * @param knowledgeBaseId 目标知识库 ID
     * @param userId           上传用户 ID
     * @return 新创建的文档记录 ID
     */
    Long uploadDocument(MultipartFile file, Long knowledgeBaseId, Long userId);

    /**
     * 校验文档数据合法性
     *
     * @param document 待校验的文档对象
     * @param add      是否为新增操作
     */
    void validDocument(Document document, boolean add);

    /**
     * 构建文档查询条件封装
     *
     * @param queryRequest 查询请求对象
     * @return 查询条件封装
     */
    LambdaQueryWrapper<Document> getQueryWrapper(DocumentQueryRequest queryRequest);

    /**
     * 获取文档视图对象
     *
     * @param document 文档实体
     * @param request  HTTP 请求
     * @return 脱敏后的文档视图
     */
    DocumentVO getDocumentVO(Document document, HttpServletRequest request);

    /**
     * 分页获取文档视图
     *
     * @param page    文档分页数据
     * @param request HTTP 请求
     * @return 文档视图分页对象
     */
    Page<DocumentVO> getDocumentVOPage(Page<Document> page, HttpServletRequest request);

    /**
     * 发送文档处理消息
     * <p>
     * 将文档 ID 发送至 MQ 队列，触发异步 ETL 处理流程。
     *
     * @param documentId 文档 ID
     */
    void sendDocumentProcessMessage(Long documentId);

    /**
     * 删除文档
     *
     * @param id          文档 ID
     * @param loginUserId 当前登录用户 ID
     * @param isAdmin    当前登录用户是否为管理员
     * @return true 表示删除成功
     */
    boolean deleteDocumentById(Long id, Long loginUserId, boolean isAdmin);
}
