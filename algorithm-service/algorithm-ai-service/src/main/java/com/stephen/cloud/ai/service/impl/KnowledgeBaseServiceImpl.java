package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.convert.KnowledgeBaseConvert;
import com.stephen.cloud.ai.mapper.KnowledgeBaseMapper;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.ai.service.KnowledgeBaseService;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    @Resource
    private UserFeignClient userFeignClient;

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
        List<KnowledgeBaseVO> voList = records.stream().map(record -> {
            KnowledgeBaseVO vo = KnowledgeBaseConvert.INSTANCE.objToVo(record);
            vo.setUserVO(finalUserMap.get(record.getUserId()));
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
}
