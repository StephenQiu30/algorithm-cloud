package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.convert.KnowledgeConvert;
import com.stephen.cloud.ai.mapper.KnowledgeBaseMapper;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.ai.service.KnowledgeService;
import com.stephen.cloud.api.knowledge.model.dto.KnowledgeBaseQueryRequest;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeBaseVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
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
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author StephenQiu30
 */
@Service
@Slf4j
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeService {

    @Resource
    private UserFeignClient userFeignClient;

    /**
     * 校验知识库合法性
     *
     * @param knowledgeBase 知识库实体
     * @param add           是否为新增操作 (新增时名称必填)
     */
    @Override
    public void validKnowledgeBase(KnowledgeBase knowledgeBase, boolean add) {
        if (knowledgeBase == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = knowledgeBase.getName();
        // 新增时，名称不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(name), ErrorCode.PARAMS_ERROR, "知识库名称不能为空");
        }
        // 字数限制
        if (StringUtils.isNotBlank(name) && name.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "名称过长");
        }
    }

    /**
     * 构造查询包装器
     *
     * @param knowledgeBaseQueryRequest 知识库查询请求对象
     * @return LambdaQueryWrapper
     */
    @Override
    public LambdaQueryWrapper<KnowledgeBase> getQueryWrapper(KnowledgeBaseQueryRequest knowledgeBaseQueryRequest) {
        LambdaQueryWrapper<KnowledgeBase> qw = new LambdaQueryWrapper<>();
        if (knowledgeBaseQueryRequest == null) {
            return qw;
        }
        Long id = knowledgeBaseQueryRequest.getId();
        String name = knowledgeBaseQueryRequest.getName();
        Long userId = knowledgeBaseQueryRequest.getUserId();
        String sortField = knowledgeBaseQueryRequest.getSortField();
        String sortOrder = knowledgeBaseQueryRequest.getSortOrder();

        // 基础字段匹配
        qw.eq(ObjectUtils.isNotEmpty(id), KnowledgeBase::getId, id);
        qw.like(StringUtils.isNotBlank(name), KnowledgeBase::getName, name);
        qw.eq(ObjectUtils.isNotEmpty(userId), KnowledgeBase::getUserId, userId);

        if (SqlUtils.validSortField(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> qw.orderBy(true, isAsc, KnowledgeBase::getCreateTime);
                case "updateTime" -> qw.orderBy(true, isAsc, KnowledgeBase::getUpdateTime);
                default -> {
                }
            }
        } else {
            qw.orderByDesc(KnowledgeBase::getUpdateTime);
        }
        return qw;
    }

    /**
     * 获取单个知识库封装
     *
     * @param knowledgeBase 实体对象
     * @param request       HTTP 请求
     * @return KnowledgeBaseVO
     */
    @Override
    public KnowledgeBaseVO getKnowledgeBaseVO(KnowledgeBase knowledgeBase, HttpServletRequest request) {
        if (knowledgeBase == null) {
            return null;
        }
        KnowledgeBaseVO vo = KnowledgeConvert.objToVo(knowledgeBase);
        // 关联用户信息
        Long userId = knowledgeBase.getUserId();
        if (userId != null && userId > 0) {
            UserVO userVO = userFeignClient.getUserVOById(userId).getData();
            vo.setUserVO(userVO);
        }
        return vo;
    }

    /**
     * 分页获取知识库封装
     *
     * @param knowledgeBasePage 实体分页对象
     * @param request           HTTP 请求
     * @return Page<KnowledgeBaseVO>
     */
    @Override
    public Page<KnowledgeBaseVO> getKnowledgeBaseVOPage(Page<KnowledgeBase> knowledgeBasePage, HttpServletRequest request) {
        List<KnowledgeBase> records = knowledgeBasePage.getRecords();
        Page<KnowledgeBaseVO> voPage = new Page<>(knowledgeBasePage.getCurrent(), knowledgeBasePage.getSize(), knowledgeBasePage.getTotal());
        if (CollUtil.isEmpty(records)) {
            return voPage;
        }

        // 1. 批量提取用户 ID
        Set<Long> userIdSet = records.stream().map(KnowledgeBase::getUserId).collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = new HashMap<>();
        
        // 2. 批量调用用户服务获取信息，减少 Feign 调用次数
        if (CollUtil.isNotEmpty(userIdSet)) {
            List<UserVO> userVOList = userFeignClient.getUserVOByIds(new ArrayList<>(userIdSet)).getData();
            if (CollUtil.isNotEmpty(userVOList)) {
                userVOMap = userVOList.stream().collect(Collectors.toMap(UserVO::getId, u -> u));
            }
        }

        // 3. 填充 VO 列表
        Map<Long, UserVO> finalUserVOMap = userVOMap;
        List<KnowledgeBaseVO> voList = records.stream().map(kb -> {
            KnowledgeBaseVO vo = KnowledgeConvert.objToVo(kb);
            vo.setUserVO(finalUserVOMap.get(kb.getUserId()));
            return vo;
        }).collect(Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }
}
