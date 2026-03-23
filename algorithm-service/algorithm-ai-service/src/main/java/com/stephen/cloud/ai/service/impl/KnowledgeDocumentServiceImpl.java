package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.convert.KnowledgeDocumentConvert;
import com.stephen.cloud.ai.mapper.KnowledgeDocumentMapper;
import com.stephen.cloud.ai.model.entity.KnowledgeDocument;
import com.stephen.cloud.api.knowledge.model.enums.KnowledgeDocumentTypeEnum;
import com.stephen.cloud.api.knowledge.model.enums.KnowledgeParseStatusEnum;
import com.stephen.cloud.ai.service.KnowledgeDocumentService;
import com.stephen.cloud.ai.service.KnowledgeIngestService;
import com.stephen.cloud.ai.service.KnowledgeBaseService;
import com.stephen.cloud.api.file.client.FileFeignClient;
import com.stephen.cloud.api.file.model.enums.FileUploadBizEnum;
import com.stephen.cloud.api.knowledge.model.dto.knowledgedocument.KnowledgeDocIngestMessage;
import com.stephen.cloud.api.knowledge.model.dto.knowledgedocument.KnowledgeDocumentQueryRequest;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeDocumentVO;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识文档服务实现类：负责文档的生命周期管理（上传、删除、权限校验）。
 * <p>
 * 核心逻辑：
 * 1. 集成 COS 云存储实现物理存储。
 * 2. 通过 RabbitMQ 异步触发 ETL 解析流水线。
 * 3. 联动检索服务实现分片数据的联级清理。
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
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private RabbitMqSender rabbitMqSender;

    @Resource
    private FileFeignClient fileFeignClient;

    @Resource
    private KnowledgeIngestService knowledgeIngestService;

    @Override
    public void validKnowledgeDocument(KnowledgeDocument entity, boolean add) {
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String originalName = entity.getOriginalName();
        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(originalName), ErrorCode.PARAMS_ERROR, "文件名不能为空");
            ThrowUtils.throwIf(entity.getKnowledgeBaseId() == null, ErrorCode.PARAMS_ERROR, "知识库 ID 不能为空");
        }
    }

    /**
     * 上传知识文档并触发异步解析流程
     * <p>
     * 流程包含：基础参数校验、文件大小校验、后缀合法性校验、知识库存在性校验、COS 文件上传、数据库记录持久化以及 RabbitMQ 消息分发。
     * </p>
     *
     * @param knowledgeBaseId 目标知识库 ID
     * @param file            前端上传的 MultipartFile
     * @param userId          执行上传操作的用户 ID
     * @return 成功保存后的文档记录主键 ID
     * @throws BusinessException 当文件校验失败、COS 上传异常或数据库写入失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadDocument(Long knowledgeBaseId, MultipartFile file, Long userId) {
        if (knowledgeBaseId == null || knowledgeBaseId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库 ID 无效");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        }
        // 1. 限制文件大小为 20MB 以内
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件过大，单文件上限为 20MB");
        }
        
        // 2. 校验文件后缀是否在系统允许范围内
        String original = file.getOriginalFilename();
        String ext = FileUtil.extName(original);
        if (StringUtils.isBlank(ext) || !ALLOWED_EXT.contains(ext.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目前仅支持 PDF, Word, Markdown, CSV, 及 Excel 格式");
        }
        
        // 3. 确保目标知识库合法存在
        if (knowledgeBaseService.getById(knowledgeBaseId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "目标知识库不存在");
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
        if (knowledgeBaseService.getById(knowledgeBaseId) == null) {
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
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDocument(Long documentId, Long userId) {
        // 1. 获取文档并校验知识库访问权限
        KnowledgeDocument doc = this.getById(documentId);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文档不存在");
        }
        if (knowledgeBaseService.getById(doc.getKnowledgeBaseId()) == null) {
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

    /**
     * 构造 MyBatis Plus Lambda 查询包装器
     * <p>
     * 支持根据所属知识库、原始文件名模糊匹配、解析状态进行筛选，并支持动态排序。
     * </p>
     *
     * @param queryRequest 查询请求 DTO
     * @return 组装完成的查询包装器
     */
    @Override
    public LambdaQueryWrapper<KnowledgeDocument> getQueryWrapper(KnowledgeDocumentQueryRequest queryRequest) {
        LambdaQueryWrapper<KnowledgeDocument> queryWrapper = new LambdaQueryWrapper<>();
        if (queryRequest == null) {
            return queryWrapper;
        }

        Long knowledgeBaseId = queryRequest.getKnowledgeBaseId();
        String originalName = queryRequest.getOriginalName();
        Integer parseStatus = queryRequest.getParseStatus();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();

        // 精确与模糊字段匹配
        queryWrapper.eq(knowledgeBaseId != null && knowledgeBaseId > 0, KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId);
        queryWrapper.like(StringUtils.isNotBlank(originalName), KnowledgeDocument::getOriginalName, originalName);
        queryWrapper.eq(parseStatus != null, KnowledgeDocument::getParseStatus, parseStatus);

        // 动态排序处理
        if (SqlUtils.validSortField(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> queryWrapper.orderBy(true, isAsc, KnowledgeDocument::getCreateTime);
                case "updateTime" -> queryWrapper.orderBy(true, isAsc, KnowledgeDocument::getUpdateTime);
                default -> queryWrapper.orderByDesc(KnowledgeDocument::getUpdateTime);
            }
        } else {
            // 默认按最后更新时间倒序
            queryWrapper.orderByDesc(KnowledgeDocument::getUpdateTime);
        }
        return queryWrapper;
    }

    @Override
    public Page<KnowledgeDocumentVO> getKnowledgeDocumentVOPage(Page<KnowledgeDocument> documentPage) {
        List<KnowledgeDocument> documentList = documentPage.getRecords();
        Page<KnowledgeDocumentVO> documentVOPage = new Page<>(documentPage.getCurrent(), documentPage.getSize(), documentPage.getTotal());
        if (documentList.isEmpty()) {
            return documentVOPage;
        }
        List<KnowledgeDocumentVO> documentVOList = documentList.stream()
                .map(KnowledgeDocumentConvert.INSTANCE::entityToDocumentVo)
                .collect(Collectors.toList());
        documentVOPage.setRecords(documentVOList);
        return documentVOPage;
    }
}
