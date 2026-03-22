package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.convert.AiChatRecordConvert;
import com.stephen.cloud.ai.mapper.AiChatRecordMapper;
import com.stephen.cloud.ai.model.entity.AiChatRecord;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordDTO;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordQueryRequest;
import com.stephen.cloud.api.ai.model.vo.AiChatRecordVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 对话记录服务实现
 * <p>
 * 提供对话历史的维护、查询、过滤以及多维度 VO 转换功能。
 * </p>
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class AiChatRecordServiceImpl extends ServiceImpl<AiChatRecordMapper, AiChatRecord>
        implements AiChatRecordService {

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private RabbitMqSender mqSender;

    /**
     * 包装并发送持久化消息到 MQ
     * <p>
     * 保持和其他微服务中实现风格的一致
     * </p>
     *
     * @param aiChatRecordDTO 记录传输对象
     */
    @Override
    public void saveAiChatRecordAsync(AiChatRecordDTO aiChatRecordDTO) {
        if (aiChatRecordDTO == null) {
            return;
        }
        try {
            Long userId = SecurityUtils.getLoginUserIdPermitNull();
            aiChatRecordDTO.setUserId(userId);
            String bizId = "ai_chat:" + System.currentTimeMillis();
            mqSender.send(MqBizTypeEnum.AI_CHAT_RECORD, bizId, aiChatRecordDTO);
        } catch (Exception e) {
            log.error("异步同步 AI 对话记录失败", e);
        }
    }

    /**
     * 校验对话记录
     *
     * @param aiChatRecord 对话记录实体
     * @param add          是否为新增
     */
    @Override
    public void validAiChatRecord(AiChatRecord aiChatRecord, boolean add) {
        if (aiChatRecord == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 对话记录必须有消息原文
        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(aiChatRecord.getMessage()), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }
    }

    /**
     * 构造组合查询条件
     *
     * @param aiChatRecordQueryRequest 查询请求
     * @return LambdaQueryWrapper
     */
    @Override
    public LambdaQueryWrapper<AiChatRecord> getQueryWrapper(AiChatRecordQueryRequest aiChatRecordQueryRequest) {
        LambdaQueryWrapper<AiChatRecord> queryWrapper = new LambdaQueryWrapper<>();
        if (aiChatRecordQueryRequest == null) {
            return queryWrapper;
        }
        Long id = aiChatRecordQueryRequest.getId();
        Long userId = aiChatRecordQueryRequest.getUserId();
        String sessionId = aiChatRecordQueryRequest.getSessionId();
        String modelType = aiChatRecordQueryRequest.getModelType();
        String searchText = aiChatRecordQueryRequest.getSearchText();
        String sortField = aiChatRecordQueryRequest.getSortField();
        String sortOrder = aiChatRecordQueryRequest.getSortOrder();

        // 精确匹配
        queryWrapper.eq(id != null && id > 0, AiChatRecord::getId, id)
                .eq(userId != null && userId > 0, AiChatRecord::getUserId, userId)
                .eq(StringUtils.isNotBlank(sessionId), AiChatRecord::getSessionId, sessionId)
                .eq(StringUtils.isNotBlank(modelType), AiChatRecord::getModelType, modelType);

        // 模糊搜索：同时匹配消息和响应内容
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw
                    .like(AiChatRecord::getMessage, searchText)
                    .or()
                    .like(AiChatRecord::getResponse, searchText));
        }

        // 排序逻辑
        if (StringUtils.isNotBlank(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> queryWrapper.orderBy(true, isAsc, AiChatRecord::getCreateTime);
                default -> {
                }
            }
        }
        return queryWrapper;
    }

    /**
     * 获取对话记录封装
     *
     * @param aiChatRecord 实体
     * @param request      HTTP 请求
     * @return AiChatRecordVO
     */
    @Override
    public AiChatRecordVO getAiChatRecordVO(AiChatRecord aiChatRecord, HttpServletRequest request) {
        if (aiChatRecord == null) {
            return null;
        }
        AiChatRecordVO vo = AiChatRecordConvert.objToVo(aiChatRecord);
        // 单个记录查询用户信息
        Long userId = aiChatRecord.getUserId();
        if (userId != null && userId > 0) {
            UserVO userVO = userFeignClient.getUserVOById(userId).getData();
            vo.setUserVO(userVO);
        }
        return vo;
    }

    /**
     * 分页获取对话记录封装
     *
     * @param aiChatRecordPage 分页实体
     * @param request          HTTP 请求
     * @return Page<AiChatRecordVO>
     */
    @Override
    public Page<AiChatRecordVO> getAiChatRecordVOPage(Page<AiChatRecord> aiChatRecordPage, HttpServletRequest request) {
        List<AiChatRecord> records = aiChatRecordPage.getRecords();
        Page<AiChatRecordVO> voPage = new Page<>(aiChatRecordPage.getCurrent(), aiChatRecordPage.getSize(),
                aiChatRecordPage.getTotal());
        if (CollUtil.isEmpty(records)) {
            return voPage;
        }

        // 1. 批量获取用户信息，极大优化性能
        Set<Long> userIdSet = records.stream().map(AiChatRecord::getUserId).collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = new HashMap<>();
        if (CollUtil.isNotEmpty(userIdSet)) {
            List<UserVO> userVOList = userFeignClient.getUserVOByIds(new ArrayList<>(userIdSet)).getData();
            if (CollUtil.isNotEmpty(userVOList)) {
                userVOMap = userVOList.stream().collect(Collectors.toMap(UserVO::getId, u -> u));
            }
        }

        // 2. 填充数据
        Map<Long, UserVO> finalUserVOMap = userVOMap;
        List<AiChatRecordVO> voList = records.stream().map(record -> {
            AiChatRecordVO vo = AiChatRecordConvert.objToVo(record);
            vo.setUserVO(finalUserVOMap.get(record.getUserId()));
            return vo;
        }).collect(Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }
}
