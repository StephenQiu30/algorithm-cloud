package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.mapper.KnowledgeDocumentMapper;
import com.stephen.cloud.ai.model.entity.KnowledgeDocument;
import com.stephen.cloud.ai.model.enums.KnowledgeParseStatusEnum;
import com.stephen.cloud.ai.service.KnowledgeDocumentService;
import com.stephen.cloud.ai.service.KnowledgeService;
import com.stephen.cloud.api.knowledge.model.dto.KnowledgeDocIngestMessage;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument>
        implements KnowledgeDocumentService {

    /**
     * 最大文件限制：20MB
     */
    private static final long MAX_BYTES = 20 * 1024 * 1024;

    /**
     * 支持并允许上传的文件后缀
     */
    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "docx", "txt", "md", "markdown");

    @Resource
    private KnowledgeService knowledgeService;

    @Resource
    private KnowledgeProperties knowledgeProperties;

    @Resource
    private RabbitMqSender rabbitMqSender;

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
        // 1. 基础参数校验
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
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目前仅支持 PDF, Word, Markdown 及纯文本");
        }
        
        // 3. 权限预检
        knowledgeService.getAndCheckAccess(knowledgeBaseId, userId);

        // 4. 内容存储：保存至本地存储目录
        File dir = FileUtil.mkdir(knowledgeProperties.getStorageDir());
        String safeName = IdUtil.simpleUUID() + "." + ext;
        File dest = new File(dir, safeName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存物理文件失败");
        }

        // 5. 数据库记录落盘
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setKnowledgeBaseId(knowledgeBaseId);
        doc.setUserId(userId);
        doc.setOriginalName(StringUtils.defaultString(original));
        doc.setStoragePath(dest.getAbsolutePath());
        doc.setMimeType(file.getContentType());
        doc.setSizeBytes(file.getSize());
        doc.setParseStatus(KnowledgeParseStatusEnum.PENDING.getValue());
        boolean result = this.save(doc);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "写入文档记录失败");
        }

        // 6. 发送异步分析消息，交由消费者进行文本提取与向量化
        KnowledgeDocIngestMessage payload = KnowledgeDocIngestMessage.builder()
                .documentId(doc.getId())
                .knowledgeBaseId(knowledgeBaseId)
                .userId(userId)
                .storagePath(dest.getAbsolutePath())
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
        // 检查用户对该知识库的访问权
        knowledgeService.getAndCheckAccess(knowledgeBaseId, userId);
        
        KnowledgeDocument doc = this.getById(documentId);
        // 核对文档是否存在且属于对应知识库
        if (doc == null || !doc.getKnowledgeBaseId().equals(knowledgeBaseId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "该文档在指定库中不存在");
        }
        return doc;
    }
}
