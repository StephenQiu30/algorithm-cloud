package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.convert.KnowledgeBaseConvert;
import com.stephen.cloud.ai.mapper.KnowledgeBaseMapper;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.ai.service.KnowledgeBaseService;
import com.stephen.cloud.api.knowledge.model.dto.knowledgebase.KnowledgeBaseQueryRequest;
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
 * 知识库服务实现类：负责知识库的 CRUD 及其关联视图对象的构建。
 * <p>
 * 遵循项目标准风格：包含参数校验、查询包装器构建及响应对象 (VO) 的高性能并行填充。
 * </p>
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    @Resource
    private UserFeignClient userFeignClient;

    /**
     * 校验知识库合法性
     * <p>
     * 包含核心字段的非空检查以及业务长度限制。
     * </p>
     *
     * @param knowledgeBase 知识库实体对象
     * @param add           是否为新增操作 (新增时 'name' 字段为必填)
     * @throws BusinessException 当校验不通过时抛出业务异常
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
        // 字数限制：名称不允许超过 50 个字符
        if (StringUtils.isNotBlank(name) && name.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "名称过长，最大支持 50 个字符");
        }
    }

    /**
     * 构造 MyBatis Plus 的 Lambda 查询包装器
     * <p>
     * 支持根据 ID 精确匹配、名称模糊查询及创建人过滤。
     * </p>
     *
     * @param knowledgeBaseQueryRequest 知识库查询请求对象
     * @return 组装好的 LambdaQueryWrapper
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

        // 动态排序处理
        if (SqlUtils.validSortField(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> qw.orderBy(true, isAsc, KnowledgeBase::getCreateTime);
                case "updateTime" -> qw.orderBy(true, isAsc, KnowledgeBase::getUpdateTime);
                default -> {
                }
            }
        } else {
            // 默认按最后更新时间倒序
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
        KnowledgeBaseVO vo = KnowledgeBaseConvert.INSTANCE.objToVo(knowledgeBase);
        // 关联用户信息
        Long userId = knowledgeBase.getUserId();
        if (userId != null && userId > 0) {
            UserVO userVO = userFeignClient.getUserVOById(userId).getData();
            vo.setUserVO(userVO);
        }
        return vo;
    }

    /**
     * 批量获取知识库封装对象 (分页)
     * <p>
     * 核心优化：采用批量 Feign 调用的方式一次性获取所有记录相关的用户信息，避免 N+1 查询问题。
     * </p>
     *
     * @param knowledgeBasePage 分页原始数据记录
     * @param request           HTTP Servlet Request
     * @return 填充完整用户信息后的封装 Page
     */
    @Override
    public Page<KnowledgeBaseVO> getKnowledgeBaseVOPage(Page<KnowledgeBase> knowledgeBasePage, HttpServletRequest request) {
        List<KnowledgeBase> records = knowledgeBasePage.getRecords();
        Page<KnowledgeBaseVO> voPage = new Page<>(knowledgeBasePage.getCurrent(), knowledgeBasePage.getSize(), knowledgeBasePage.getTotal());
        if (CollUtil.isEmpty(records)) {
            return voPage;
        }

        // 1. 批量提取非重的用户 ID
        Set<Long> userIdSet = records.stream().map(KnowledgeBase::getUserId).collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = new HashMap<>();
        
        // 2. 批量调用用户服务获取信息，减少 Feign 往返开销
        if (CollUtil.isNotEmpty(userIdSet)) {
            List<UserVO> userVOList = userFeignClient.getUserVOByIds(new ArrayList<>(userIdSet)).getData();
            if (CollUtil.isNotEmpty(userVOList)) {
                userVOMap = userVOList.stream().collect(Collectors.toMap(UserVO::getId, u -> u));
            }
        }

        // 3. 将实体转换为 VO 并注入用户信息
        Map<Long, UserVO> finalUserVOMap = userVOMap;
        List<KnowledgeBaseVO> voList = records.stream().map(kb -> {
            KnowledgeBaseVO vo = KnowledgeBaseConvert.INSTANCE.objToVo(kb);
            vo.setUserVO(finalUserVOMap.get(kb.getUserId()));
            return vo;
        }).collect(Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }
}
