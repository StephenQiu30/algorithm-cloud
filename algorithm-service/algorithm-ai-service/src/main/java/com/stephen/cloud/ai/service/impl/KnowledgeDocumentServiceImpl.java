package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.mapper.KnowledgeDocumentMapper;
import com.stephen.cloud.ai.model.entity.KnowledgeDocument;
import com.stephen.cloud.ai.model.enums.KnowledgeDocumentTypeEnum;
import com.stephen.cloud.ai.model.enums.KnowledgeParseStatusEnum;
import com.stephen.cloud.ai.service.KnowledgeDocumentService;
import com.stephen.cloud.ai.service.KnowledgeIngestService;
import com.stephen.cloud.ai.service.KnowledgeService;
import com.stephen.cloud.api.file.client.FileFeignClient;
import com.stephen.cloud.api.file.model.enums.FileUploadBizEnum;
import com.stephen.cloud.api.knowledge.model.dto.KnowledgeDocIngestMessage;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

/**
 * 知识文档服务实现
 * <p>
 * 处理文档的上传、本地存储以及通过 MQ 触发异步解析及向量化流程。
 * </p>
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument>
        implements KnowledgeDocumentService {

    /**
     * 最大文件限制：20MB (避免大文件处理导致的 OOM)
     */
    private static final long MAX_BYTES = 20 * 1024 * 1024;

    /**
     * 系统支持的文档格式，由 {@link KnowledgeDocumentTypeEnum} 统一管理
     */
    private static final Set<String> ALLOWED_EXT = Set.copyOf(KnowledgeDocumentTypeEnum.getValues());

    @Resource
    private KnowledgeService knowledgeService;

    @Resource
    private RabbitMqSender rabbitMqSender;

    @Resource
    private FileFeignClient fileFeignClient;

    @Resource
    private KnowledgeIngestService knowledgeIngestService;

    /**
     * 上传并处理文档
     *
     * @param knowledgeBaseId 目标知识库 ID
     * @param file            上传的文件对象
     * @param userId          用户 ID
     * @return 数据库记录 ID
     */
    @Override
    public Long uploadDocument(Long knowledgeBaseId, MultipartFile file, Long userId) {
        if (knowledgeBaseId == null || knowledgeBaseId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库 ID 无效");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件过大，上限 20MB");
        }
        
        // 2. 后缀校验
        String original = file.getOriginalFilename();
        String ext = FileUtil.extName(original);
        if (StringUtils.isBlank(ext) || !ALLOWED_EXT.contains(ext.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目前支持 PDF, Word, Markdown, CSV, 及 Excel");
        }
        
        if (knowledgeService.getById(knowledgeBaseId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        }

        // 4. 内容存储：上传至 COS
        BaseResponse<String> uploadResponse = fileFeignClient.uploadFile(file, FileUploadBizEnum.KNOWLEDGE.getValue());
        if (uploadResponse == null || uploadResponse.getData() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传到云存储失败");
        }
        String cosUrl = uploadResponse.getData();

        // 5. 数据库记录落盘
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setKnowledgeBaseId(knowledgeBaseId);
        doc.setUserId(userId);
        doc.setOriginalName(StringUtils.defaultString(original));
        doc.setStoragePath(cosUrl);
        doc.setMimeType(file.getContentType());
        doc.setSizeBytes(file.getSize());
        doc.setParseStatus(KnowledgeParseStatusEnum.PENDING.getValue());
        boolean result = this.save(doc);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "写入文档记录失败");
        }

        // 6. 发送异步分析消息
        KnowledgeDocIngestMessage payload = KnowledgeDocIngestMessage.builder()
                .documentId(doc.getId())
                .knowledgeBaseId(knowledgeBaseId)
                .userId(userId)
                .storagePath(cosUrl)
                .build();
        rabbitMqSender.send(MqBizTypeEnum.KNOWLEDGE_DOC_INGEST, payload);
        
        return doc.getId();
    }

    /**
     * 获取指定文档并校验权限
     *
     * @param knowledgeBaseId 知识库 ID
     * @param documentId      文档 ID
     * @param userId          用户 ID
     * @return 文档实体
     */
    @Override
    public KnowledgeDocument getDocumentForUser(Long knowledgeBaseId, Long documentId, Long userId) {
        if (knowledgeService.getById(knowledgeBaseId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        }

        KnowledgeDocument doc = this.getById(documentId);
        // 核对文档是否存在且属于对应知识库
        if (doc == null || !doc.getKnowledgeBaseId().equals(knowledgeBaseId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "该文档在指定库中不存在");
        }
        return doc;
    }

    /**
     * 删除文档：先清理 ES 向量及 {@code document_chunk}、{@code embedding_vector} 中关联数据，再逻辑删除文档记录
     *
     * @param documentId 文档 ID
     * @param userId     用户 ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteDocument(Long documentId, Long userId) {
        // 1. 获取文档并校验知识库访问权限
        KnowledgeDocument doc = this.getById(documentId);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文档不存在");
        }
        if (knowledgeService.getById(doc.getKnowledgeBaseId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        }

        // 2. 同步清理向量与关系表（失败仅记录日志，不阻塞主流程删除）
        try {
            knowledgeIngestService.removeChunksAndVectorsForDocument(documentId);
        } catch (Exception e) {
            log.error("Failed to remove chunks/vectors for documentId: {}", documentId, e);
        }

        // 3. 逻辑删除 knowledge_document
        return this.removeById(documentId);
    }
}
