package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.convert.KnowledgeBaseConvert;
import com.stephen.cloud.ai.knowledge.retrieval.VectorSearchManager;
import com.stephen.cloud.ai.mapper.KnowledgeBaseMapper;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.ai.model.entity.KnowledgeDocument;
import com.stephen.cloud.ai.mq.KnowledgeIngestMqProducer;
import com.stephen.cloud.ai.service.DocumentChunkService;
import com.stephen.cloud.ai.service.KnowledgeBaseService;
import com.stephen.cloud.ai.service.KnowledgeDocumentService;
import com.stephen.cloud.ai.service.KnowledgeIngestService;
import com.stephen.cloud.api.file.client.FileFeignClient;
import com.stephen.cloud.api.file.model.enums.FileUploadBizEnum;
import com.stephen.cloud.api.file.model.vo.FileUploadVO;
import com.stephen.cloud.api.knowledge.model.dto.knowledgedocument.KnowledgeDocIngestMessage;
import com.stephen.cloud.api.knowledge.model.dto.knowledgebase.KnowledgeBaseQueryRequest;
import com.stephen.cloud.api.knowledge.model.dto.retrieval.KnowledgeRetrievalRequest;
import com.stephen.cloud.api.knowledge.model.enums.KnowledgeDocumentTypeEnum;
import com.stephen.cloud.api.knowledge.model.enums.KnowledgeParseStatusEnum;
import com.stephen.cloud.api.knowledge.model.enums.VectorSimilarityModeEnum;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeBaseVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统一知识库业务实现类。
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    @Resource
    private UserFeignClient userFeignClient;
    @Resource
    private FileFeignClient fileFeignClient;
    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;
    @Resource
    private DocumentChunkService documentChunkService;
    @Resource
    private KnowledgeProperties knowledgeProperties;
    @Resource
    private KnowledgeIngestMqProducer knowledgeIngestMqProducer;
    @Resource
    private VectorSearchManager vectorSearchManager;
    @Resource
    private KnowledgeIngestService knowledgeIngestService;

    private static final long MAX_FILE_BYTES = 20 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.copyOf(KnowledgeDocumentTypeEnum.getValues());

    @Override
    public void validKnowledgeBase(KnowledgeBase knowledgeBase, boolean add) {
        if (knowledgeBase == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        String name = knowledgeBase.getName();
        if (add) ThrowUtils.throwIf(StringUtils.isBlank(name), ErrorCode.PARAMS_ERROR, "知识库名称不能为空");
        if (StringUtils.isNotBlank(name) && name.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "名称过长，最大支持 50 个字符");
        }
    }

    @Override
    public LambdaQueryWrapper<KnowledgeBase> getQueryWrapper(KnowledgeBaseQueryRequest queryRequest) {
        LambdaQueryWrapper<KnowledgeBase> qw = new LambdaQueryWrapper<>();
        if (queryRequest == null) return qw;
        qw.eq(ObjectUtils.isNotEmpty(queryRequest.getId()), KnowledgeBase::getId, queryRequest.getId());
        qw.like(StringUtils.isNotBlank(queryRequest.getName()), KnowledgeBase::getName, queryRequest.getName());
        qw.eq(ObjectUtils.isNotEmpty(queryRequest.getUserId()), KnowledgeBase::getUserId, queryRequest.getUserId());
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();
        if (SqlUtils.validSortField(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> qw.orderBy(true, isAsc, KnowledgeBase::getCreateTime);
                case "updateTime" -> qw.orderBy(true, isAsc, KnowledgeBase::getUpdateTime);
            }
        } else {
            qw.orderByDesc(KnowledgeBase::getUpdateTime);
        }
        return qw;
    }

    @Override
    public KnowledgeBaseVO getKnowledgeBaseVO(KnowledgeBase knowledgeBase, HttpServletRequest request) {
        if (knowledgeBase == null) return null;
        KnowledgeBaseVO vo = KnowledgeBaseConvert.INSTANCE.objToVo(knowledgeBase);
        Long userId = knowledgeBase.getUserId();
        if (userId != null && userId > 0) {
            vo.setUserVO(userFeignClient.getUserVOById(userId).getData());
        }
        return vo;
    }

    @Override
    public Page<KnowledgeBaseVO> getKnowledgeBaseVOPage(Page<KnowledgeBase> page, HttpServletRequest request) {
        List<KnowledgeBase> records = page.getRecords();
        Page<KnowledgeBaseVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isEmpty(records)) return voPage;
        Set<Long> userIdSet = records.stream().map(KnowledgeBase::getUserId).collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = new HashMap<>();
        if (CollUtil.isNotEmpty(userIdSet)) {
            List<UserVO> userVOList = userFeignClient.getUserVOByIds(new ArrayList<>(userIdSet)).getData();
            if (CollUtil.isNotEmpty(userVOList)) {
                userVOMap = userVOList.stream().collect(Collectors.toMap(UserVO::getId, u -> u));
            }
        }
        Map<Long, UserVO> finalUserVOMap = userVOMap;
        List<KnowledgeBaseVO> voList = records.stream().map(kb -> {
            KnowledgeBaseVO vo = KnowledgeBaseConvert.INSTANCE.objToVo(kb);
            vo.setUserVO(finalUserVOMap.get(kb.getUserId()));
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadDocument(Long knowledgeBaseId, MultipartFile file, Long userId) {
        if (knowledgeBaseId == null || knowledgeBaseId <= 0) throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库 ID 无效");
        if (file == null || file.isEmpty()) throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        if (file.getSize() > MAX_FILE_BYTES) throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件过大，上限 20MB");
        
        String original = StringUtils.defaultString(file.getOriginalFilename());
        log.info("[知识库] 开始上传文档: kbId={}, name={}, size={} 字节, userId={}", 
                knowledgeBaseId, original, file.getSize(), userId);
        
        String ext = FileUtil.extName(original).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) {
            log.error("[知识库] 不支持的文件格式: {} (kbId={})", ext, knowledgeBaseId);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持该文件格式: " + ext);
        }
        
        if (this.getById(knowledgeBaseId) == null) {
            log.error("[知识库] 目标知识库不存在: kbId={}", knowledgeBaseId);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "目标知识库不存在");
        }

        log.info("[知识库] 调用文件服务存储文档: kbId={}", knowledgeBaseId);
        BaseResponse<FileUploadVO> uploadResponse = fileFeignClient.uploadFile(file, FileUploadBizEnum.KNOWLEDGE.getValue());
        String cosUrl = Optional.ofNullable(uploadResponse).map(BaseResponse::getData).map(FileUploadVO::getUrl)
                .orElseThrow(() -> {
                    log.error("[知识库] 文件服务上传失败: kbId={}", knowledgeBaseId);
                    return new BusinessException(ErrorCode.OPERATION_ERROR, "文件存储上传失败");
                });

        log.info("[知识库] 创建知识文档记录: kbId={}, url={}", knowledgeBaseId, cosUrl);
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setKnowledgeBaseId(knowledgeBaseId);
        doc.setUserId(userId);
        doc.setOriginalName(original);
        doc.setStoragePath(cosUrl);
        doc.setMimeType(file.getContentType());
        doc.setSizeBytes(file.getSize());
        doc.setParseStatus(KnowledgeParseStatusEnum.PENDING.getValue());
        if (!knowledgeDocumentService.save(doc)) {
            log.error("[知识库] 数据库保存文档记录失败: {}", original);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "写入文档记录失败");
        }

        dispatchIngestMessageAfterCommit(doc, knowledgeBaseId, userId, cosUrl);
        
        return doc.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDocumentAndAssociated(Long documentId, Long userId) {
        log.info("[知识库] 请求删除文档及关联数据: docId={}, userId={}", documentId, userId);
        knowledgeIngestService.deleteVectors(documentId);
        log.debug("[知识库] 向量库关联向量已清理: docId={}", documentId);
        documentChunkService.deleteByDocumentId(documentId);
        log.debug("[知识库] 数据库关联分块已清理: docId={}", documentId);
        boolean removed = knowledgeDocumentService.removeById(documentId);
        log.info("[知识库] 文档记录清理结果: {}, docId={}", removed, documentId);
        return removed;
    }

    @Override
    public List<ChunkSourceVO> searchChunks(KnowledgeRetrievalRequest request, Long userId) {
        if (request == null || StringUtils.isBlank(request.getQuery())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "检索内容不能为空");
        }
        int topK = request.getTopK() != null ? request.getTopK() : knowledgeProperties.getDefaultTopK();
        int maxK = knowledgeProperties.getRetrievalTopKMax();
        int finalTopK = Math.min(topK, maxK);
        
        log.info("[知识库] 执行分块检索: query='{}', kbId={}, topK={}, userId={}", 
                request.getQuery(), request.getKnowledgeBaseId(), finalTopK, userId);
        
        SearchRequest searchRequest = SearchRequest.builder()
                .query(request.getQuery())
                .topK(finalTopK)
                .filterExpression("knowledgeBaseId == '" + request.getKnowledgeBaseId() + "'")
                .build();
        
        List<Document> docs = similaritySearch(searchRequest);
        log.info("[知识库] 检索返回文档数: {} (kbId={})", docs.size(), request.getKnowledgeBaseId());
        
        return vectorSearchManager.mapToVO(docs);
    }

    @Override
    public Map<String, List<ChunkSourceVO>> diagnoseHybridSearch(KnowledgeRetrievalRequest request, Long userId) {
        if (request == null || StringUtils.isBlank(request.getQuery())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "检索内容不能为空");
        }
        ThrowUtils.throwIf(request.getKnowledgeBaseId() == null || request.getKnowledgeBaseId() <= 0,
                ErrorCode.PARAMS_ERROR, "知识库 ID 非法");
        int topK = request.getTopK() != null ? request.getTopK() : knowledgeProperties.getDefaultTopK();
        int maxK = knowledgeProperties.getRetrievalTopKMax();
        int finalTopK = Math.min(topK, maxK);
        SearchRequest searchRequest = SearchRequest.builder()
                .query(request.getQuery())
                .topK(finalTopK)
                .similarityThreshold(knowledgeProperties.getSimilarityThreshold())
                .filterExpression("knowledgeBaseId == '" + request.getKnowledgeBaseId() + "'")
                .build();
        Map<String, List<Document>> diagnostics = vectorSearchManager.diagnoseHybrid(searchRequest);
        Map<String, List<ChunkSourceVO>> result = new LinkedHashMap<>();
        result.put("knn", vectorSearchManager.mapToVO(diagnostics.getOrDefault("knn", List.of())));
        result.put("bm25", vectorSearchManager.mapToVO(diagnostics.getOrDefault("bm25", List.of())));
        result.put("hybrid", vectorSearchManager.mapToVO(diagnostics.getOrDefault("hybrid", List.of())));
        log.info("[知识库] 双路召回诊断完成: kbId={}, query='{}', knn={}, bm25={}, hybrid={}",
                request.getKnowledgeBaseId(),
                request.getQuery(),
                result.get("knn").size(),
                result.get("bm25").size(),
                result.get("hybrid").size());
        return result;
    }

    @Override
    public List<Document> similaritySearch(SearchRequest searchRequest) {
        return vectorSearchManager.search(searchRequest,
                knowledgeProperties.isHybridSearchEnabled() ?
                        VectorSimilarityModeEnum.HYBRID :
                        VectorSimilarityModeEnum.KNN);
    }

    private void dispatchIngestMessageAfterCommit(KnowledgeDocument doc, Long knowledgeBaseId, Long userId, String storagePath) {
        KnowledgeDocIngestMessage message = KnowledgeDocIngestMessage.builder()
                .documentId(doc.getId())
                .knowledgeBaseId(knowledgeBaseId)
                .userId(userId)
                .storagePath(storagePath)
                .build();
        Runnable sendTask = () -> {
            log.info("[知识库] 自动触发文档解析入库: docId={}, kbId={}", doc.getId(), knowledgeBaseId);
            knowledgeIngestMqProducer.sendIngestCreated(message);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendTask.run();
                }
            });
        } else {
            sendTask.run();
        }
    }
}
