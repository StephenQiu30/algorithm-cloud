package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.convert.KnowledgeBaseConvert;
import com.stephen.cloud.ai.mapper.DocumentChunkMapper;
import com.stephen.cloud.ai.mapper.KnowledgeBaseMapper;
import com.stephen.cloud.ai.model.entity.Document;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.ai.service.DocumentService;
import com.stephen.cloud.ai.service.KnowledgeBaseService;
import com.stephen.cloud.ai.service.VectorStoreService;
import com.stephen.cloud.api.ai.model.dto.knowledgebase.KnowledgeBaseQueryRequest;
import com.stephen.cloud.api.ai.model.vo.KnowledgeBaseVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库管理服务实现
 * <p>
 * 提供知识库的增删改查、数据校验、视图转换等能力
 * </p>
 *
 * @author StephenQiu30
 */
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private DocumentService documentService;

    @Resource
    private VectorStoreService vectorStoreService;

    @Resource
    private DocumentChunkMapper documentChunkMapper;

    @Override
    public void validKnowledgeBase(KnowledgeBase knowledgeBase, boolean add) {
        if (knowledgeBase == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = knowledgeBase.getName();
        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(name), ErrorCode.PARAMS_ERROR, "知识库名称不能为空");
        }
        if (StringUtils.isNotBlank(name)) {
            ThrowUtils.throwIf(name.length() > 100, ErrorCode.PARAMS_ERROR, "知识库名称过长");
        }
    }

    @Override
    public LambdaQueryWrapper<KnowledgeBase> getQueryWrapper(KnowledgeBaseQueryRequest queryRequest) {
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        if (queryRequest == null) {
            return queryWrapper;
        }
        queryWrapper.eq(queryRequest.getId() != null && queryRequest.getId() > 0, KnowledgeBase::getId, queryRequest.getId())
                .eq(queryRequest.getUserId() != null && queryRequest.getUserId() > 0, KnowledgeBase::getUserId, queryRequest.getUserId())
                .like(StringUtils.isNotBlank(queryRequest.getName()), KnowledgeBase::getName, queryRequest.getName());
        if (StringUtils.isNotBlank(queryRequest.getSearchText())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(KnowledgeBase::getName, queryRequest.getSearchText())
                    .or()
                    .like(KnowledgeBase::getDescription, queryRequest.getSearchText()));
        }
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();
        if (StringUtils.isNotBlank(sortField) && "createTime".equals(sortField)) {
            queryWrapper.orderBy(true, CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder), KnowledgeBase::getCreateTime);
        } else {
            queryWrapper.orderByDesc(KnowledgeBase::getCreateTime);
        }
        return queryWrapper;
    }

    @Override
    public KnowledgeBaseVO getKnowledgeBaseVO(KnowledgeBase knowledgeBase, HttpServletRequest request) {
        if (knowledgeBase == null) {
            return null;
        }
        KnowledgeBaseVO knowledgeBaseVO = KnowledgeBaseConvert.INSTANCE.objToVo(knowledgeBase);
        // 确保文档数量准确，直接从文档表查询
        long documentCount = documentService.count(new LambdaQueryWrapper<Document>().eq(Document::getKnowledgeBaseId, knowledgeBase.getId()));
        knowledgeBaseVO.setDocumentCount((int) documentCount);
        Long userId = knowledgeBase.getUserId();
        if (userId != null && userId > 0) {
            UserVO userVO = userFeignClient.getUserVOById(userId).getData();
            knowledgeBaseVO.setUserVO(userVO);
        }
        return knowledgeBaseVO;
    }

    @Override
    public Page<KnowledgeBaseVO> getKnowledgeBaseVOPage(Page<KnowledgeBase> page, HttpServletRequest request) {
        List<KnowledgeBase> records = page.getRecords();
        Page<KnowledgeBaseVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isEmpty(records)) {
            return voPage;
        }
        Set<Long> userIds = records.stream().map(KnowledgeBase::getUserId).collect(Collectors.toSet());
        Map<Long, UserVO> userMap = new HashMap<>();
        if (CollUtil.isNotEmpty(userIds)) {
            List<UserVO> userVOList = userFeignClient.getUserVOByIds(new ArrayList<>(userIds)).getData();
            if (CollUtil.isNotEmpty(userVOList)) {
                userMap = userVOList.stream().collect(Collectors.toMap(UserVO::getId, item -> item));
            }
        }
        Map<Long, UserVO> finalUserMap = userMap;

        // 批量获取文档数量以提高性能
        List<Long> kbIds = records.stream().map(KnowledgeBase::getId).collect(Collectors.toList());
        Map<Long, Long> kbDocumentCountMap = new HashMap<>();
        if (CollUtil.isNotEmpty(kbIds)) {
            // 使用分组查询获取每个知识库的文档数量，提高查询效率
            List<Map<String, Object>> countMaps = documentService.listMaps(new QueryWrapper<Document>()
                    .select("knowledge_base_id", "COUNT(*) AS doc_count")
                    .lambda()
                    .in(Document::getKnowledgeBaseId, kbIds)
                    .groupBy(Document::getKnowledgeBaseId));

            kbDocumentCountMap = countMaps.stream().collect(Collectors.toMap(
                    map -> (Long) map.get("knowledge_base_id"),
                    map -> (Long) map.get("doc_count")
            ));
        }
        Map<Long, Long> finalKbDocumentCountMap = kbDocumentCountMap;

        List<KnowledgeBaseVO> voList = records.stream().map(record -> {
            KnowledgeBaseVO vo = KnowledgeBaseConvert.INSTANCE.objToVo(record);
            vo.setUserVO(finalUserMap.get(record.getUserId()));
            // 设置实时查询的文档数量
            vo.setDocumentCount(finalKbDocumentCountMap.getOrDefault(record.getId(), 0L).intValue());
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public boolean isNameUnique(String name, Long excludeId) {
        if (StringUtils.isBlank(name)) {
            return false;
        }
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getName, name);
        if (excludeId != null && excludeId > 0) {
            queryWrapper.ne(KnowledgeBase::getId, excludeId);
        }
        return this.count(queryWrapper) == 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteKnowledgeBaseById(Long id, Long loginUserId, boolean isAdmin) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        KnowledgeBase oldKnowledgeBase = this.getById(id);
        ThrowUtils.throwIf(oldKnowledgeBase == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!Objects.equals(oldKnowledgeBase.getUserId(), loginUserId) && !isAdmin, ErrorCode.NO_AUTH_ERROR);
        vectorStoreService.deleteByKnowledgeBaseId(id);
        documentChunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getKnowledgeBaseId, id));
        documentService.remove(new LambdaQueryWrapper<Document>().eq(Document::getKnowledgeBaseId, id));
        return this.removeById(id);
    }
}
