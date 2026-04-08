package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.config.DocumentProcessingProperties;
import com.stephen.cloud.ai.convert.DocumentConvert;
import com.stephen.cloud.ai.mapper.DocumentChunkMapper;
import com.stephen.cloud.ai.mapper.DocumentMapper;
import com.stephen.cloud.ai.model.entity.Document;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.mq.DocumentProcessProducer;
import com.stephen.cloud.ai.mq.model.DocumentProcessMessage;
import com.stephen.cloud.ai.service.ChunkService;
import com.stephen.cloud.ai.service.DocumentService;
import com.stephen.cloud.ai.service.VectorStoreService;
import com.stephen.cloud.api.ai.model.dto.document.DocumentQueryRequest;
import com.stephen.cloud.api.ai.model.enums.DocumentParseStatusEnum;
import com.stephen.cloud.api.ai.model.vo.DocumentVO;
import com.stephen.cloud.api.file.client.FileFeignClient;
import com.stephen.cloud.api.file.model.enums.FileUploadBizEnum;
import com.stephen.cloud.api.file.model.vo.FileUploadVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档管理服务实现
 * <p>
 * 提供文档的上传、删除、查询等功能，并触发异步 ETL 处理流程
 * </p>
 *
 * @author StephenQiu30
 */
@Service
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "doc", "docx", "md", "txt", "ppt", "pptx", "html");

    @Resource
    private DocumentProcessingProperties documentProcessingProperties;

    @Resource
    private DocumentProcessProducer documentProcessProducer;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private VectorStoreService vectorStoreService;

    @Resource
    private FileFeignClient fileFeignClient;

    @Resource
    private DocumentChunkMapper documentChunkMapper;

    @Resource
    private ChunkService chunkService;

    @Override
    public Long uploadDocument(MultipartFile file, Long knowledgeBaseId, Long userId) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");
        ThrowUtils.throwIf(knowledgeBaseId == null || knowledgeBaseId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(file.getSize() > documentProcessingProperties.getMaxFileSize(), ErrorCode.PARAMS_ERROR, "文件大小超过限制");
        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.lowerCase(FilenameUtils.getExtension(originalFilename));
        ThrowUtils.throwIf(StringUtils.isBlank(extension) || !SUPPORTED_EXTENSIONS.contains(extension),
                ErrorCode.PARAMS_ERROR, "不支持的文件格式");
        BaseResponse<FileUploadVO> uploadResponse = fileFeignClient.uploadFile(file, FileUploadBizEnum.KNOWLEDGE.getValue());
        ThrowUtils.throwIf(uploadResponse == null || uploadResponse.getCode() != 0
                || uploadResponse.getData() == null || StringUtils.isBlank(uploadResponse.getData().getUrl()),
                ErrorCode.OPERATION_ERROR, "文件保存失败");

        String fileUrl = uploadResponse.getData().getUrl();
        Document document = new Document();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setName(StringUtils.defaultIfBlank(originalFilename, uploadResponse.getData().getFileName()));
        document.setFilePath(fileUrl);
        document.setFileSize(file.getSize());
        document.setFileExtension(extension);
        document.setStatus(DocumentParseStatusEnum.PENDING.getValue());
        document.setChunkCount(0);
        document.setUserId(userId);
        document.setUploadTime(new Date());
        boolean result = this.save(document);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        sendDocumentProcessMessage(document.getId());
        return document.getId();
    }

    @Override
    public void validDocument(Document document, boolean add) {
        if (document == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (add) {
            ThrowUtils.throwIf(document.getKnowledgeBaseId() == null || document.getKnowledgeBaseId() <= 0, ErrorCode.PARAMS_ERROR);
            ThrowUtils.throwIf(StringUtils.isBlank(document.getName()), ErrorCode.PARAMS_ERROR, "文档名称不能为空");
        }
    }

    @Override
    public LambdaQueryWrapper<Document> getQueryWrapper(DocumentQueryRequest queryRequest) {
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        if (queryRequest == null) {
            return queryWrapper;
        }
        queryWrapper.eq(queryRequest.getId() != null && queryRequest.getId() > 0, Document::getId, queryRequest.getId())
                .eq(queryRequest.getKnowledgeBaseId() != null && queryRequest.getKnowledgeBaseId() > 0, Document::getKnowledgeBaseId, queryRequest.getKnowledgeBaseId())
                .eq(queryRequest.getUserId() != null && queryRequest.getUserId() > 0, Document::getUserId, queryRequest.getUserId())
                .eq(StringUtils.isNotBlank(queryRequest.getStatus()), Document::getStatus, queryRequest.getStatus())
                .like(StringUtils.isNotBlank(queryRequest.getName()), Document::getName, queryRequest.getName());
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();
        if (StringUtils.isNotBlank(sortField) && "createTime".equals(sortField)) {
            queryWrapper.orderBy(true, CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder), Document::getCreateTime);
        } else {
            queryWrapper.orderByDesc(Document::getCreateTime);
        }
        return queryWrapper;
    }

    @Override
    public DocumentVO getDocumentVO(Document document, HttpServletRequest request) {
        if (document == null) {
            return null;
        }
        DocumentVO documentVO = DocumentConvert.INSTANCE.objToVo(document);
        Long userId = document.getUserId();
        if (userId != null && userId > 0) {
            UserVO userVO = userFeignClient.getUserVOById(userId).getData();
            documentVO.setUserVO(userVO);
        }
        return documentVO;
    }

    @Override
    public Page<DocumentVO> getDocumentVOPage(Page<Document> page, HttpServletRequest request) {
        List<Document> records = page.getRecords();
        Page<DocumentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isEmpty(records)) {
            return voPage;
        }
        Set<Long> userIds = records.stream().map(Document::getUserId).collect(Collectors.toSet());
        Map<Long, UserVO> userMap = new HashMap<>();
        if (CollUtil.isNotEmpty(userIds)) {
            List<UserVO> userVOList = userFeignClient.getUserVOByIds(new ArrayList<>(userIds)).getData();
            if (CollUtil.isNotEmpty(userVOList)) {
                userMap = userVOList.stream().collect(Collectors.toMap(UserVO::getId, item -> item));
            }
        }
        Map<Long, UserVO> finalUserMap = userMap;
        List<DocumentVO> voList = records.stream().map(record -> {
            DocumentVO vo = DocumentConvert.INSTANCE.objToVo(record);
            vo.setUserVO(finalUserMap.get(record.getUserId()));
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public void sendDocumentProcessMessage(Long documentId) {
        if (documentId == null || documentId <= 0) {
            return;
        }
        Document document = this.getById(documentId);
        if (document == null) {
            return;
        }
        DocumentProcessMessage message = new DocumentProcessMessage();
        message.setDocumentId(document.getId());
        message.setKnowledgeBaseId(document.getKnowledgeBaseId());
        message.setFilePath(document.getFilePath());
        message.setFileExtension(document.getFileExtension());
        documentProcessProducer.sendMessage(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDocumentById(Long id, Long loginUserId, boolean isAdmin) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Document oldDocument = this.getById(id);
        ThrowUtils.throwIf(oldDocument == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!Objects.equals(oldDocument.getUserId(), loginUserId) && !isAdmin, ErrorCode.NO_AUTH_ERROR);
        vectorStoreService.deleteByDocumentId(id);
        List<Long> chunkIds = documentChunkMapper.selectList(new LambdaQueryWrapper<DocumentChunk>()
                        .select(DocumentChunk::getId)
                        .eq(DocumentChunk::getDocumentId, id))
                .stream()
                .map(DocumentChunk::getId)
                .filter(Objects::nonNull)
                .toList();
        documentChunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getDocumentId, id));
        for (Long chunkId : chunkIds) {
            chunkService.syncToEs(chunkId);
        }
        return this.removeById(id);
    }
}
