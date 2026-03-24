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
     * 异步持久化 AI 对话记录
     * <p>
     * 通过消息队列 (RabbitMQ) 实现记录的削峰填谷，确保持久化操作不阻塞主对话流程。
     * </p>
     *
     * @param aiChatRecordDTO 包含对话详情的数据传输对象
     */
    @Override
    public void saveAiChatRecordAsync(AiChatRecordDTO aiChatRecordDTO) {
        if (aiChatRecordDTO == null) {
            return;
        }
        try {
            // 自动注入当前登录用户 ID (如有)
            Long userId = SecurityUtils.getLoginUserIdPermitNull();
            aiChatRecordDTO.setUserId(userId);
            // 生成追踪用的业务 ID
            String bizId = "ai_chat:" + System.currentTimeMillis();
            mqSender.send(MqBizTypeEnum.AI_CHAT_RECORD, bizId, aiChatRecordDTO);
            log.info("已成功发送对话记录异步持久化消息: bizId={}", bizId);
        } catch (Exception e) {
            log.error("尝试发送异步对话记录失败: {}", e.getMessage(), e);
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
     * 构造 MyBatis Plus 的 Lambda 查询条件封装
     * <p>
     * 支持多字段组合查询，包含对 'message' 和 'response' 的 OR 模糊匹配搜索。
     * </p>
     *
     * @param aiChatRecordQueryRequest 查询请求包装类
     * @return 组装完成的 LambdaQueryWrapper 对象
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

        // 1. 执行精准字段过滤
        queryWrapper.eq(id != null && id > 0, AiChatRecord::getId, id)
                .eq(userId != null && userId > 0, AiChatRecord::getUserId, userId)
                .eq(StringUtils.isNotBlank(sessionId), AiChatRecord::getSessionId, sessionId)
                .eq(StringUtils.isNotBlank(modelType), AiChatRecord::getModelType, modelType);

        // 2. 复合搜索关键词匹配 (消息原文 OR AI 回复内容)
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw
                    .like(AiChatRecord::getMessage, searchText)
                    .or()
                    .like(AiChatRecord::getResponse, searchText));
        }

        // 3. 处理分页排序规则
        if (StringUtils.isNotBlank(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> queryWrapper.orderBy(true, isAsc, AiChatRecord::getCreateTime);
                default -> {
                }
            }
        } else {
            // 默认按创建时间倒序排
            queryWrapper.orderByDesc(AiChatRecord::getCreateTime);
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
        AiChatRecordVO aiChatRecordVO = AiChatRecordConvert.INSTANCE.objToVo(aiChatRecord);
        // 单个记录查询用户信息
        Long userId = aiChatRecord.getUserId();
        if (userId != null && userId > 0) {
            UserVO userVO = userFeignClient.getUserVOById(userId).getData();
            aiChatRecordVO.setUserVO(userVO);
        }
        return aiChatRecordVO;
    }

    /**
     * 批量获取对话记录封装 VO (分页)
     * <p>
     * 使用批量获取 UserVO 的策略来规避单记录循环请求带来的性能损耗。
     * </p>
     *
     * @param aiChatRecordPage 原始分页结果
     * @param request          HTTP 请求
     * @return 包含用户信息的数据视图分页对象
     */
    @Override
    public Page<AiChatRecordVO> getAiChatRecordVOPage(Page<AiChatRecord> aiChatRecordPage, HttpServletRequest request) {
        List<AiChatRecord> records = aiChatRecordPage.getRecords();
        Page<AiChatRecordVO> voPage = new Page<>(aiChatRecordPage.getCurrent(), aiChatRecordPage.getSize(),
                aiChatRecordPage.getTotal());
        if (CollUtil.isEmpty(records)) {
            return voPage;
        }

        // 1. 批量提取并去重用户 ID，极大优化 RPC 性能
        Set<Long> userIdSet = records.stream().map(AiChatRecord::getUserId).collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = new HashMap<>();
        if (CollUtil.isNotEmpty(userIdSet)) {
            List<UserVO> userVOList = userFeignClient.getUserVOByIds(new ArrayList<>(userIdSet)).getData();
            if (CollUtil.isNotEmpty(userVOList)) {
                userVOMap = userVOList.stream().collect(Collectors.toMap(UserVO::getId, u -> u));
            }
        }

        // 2. 将实体转化为 VO 并填充关联用户信息
        Map<Long, UserVO> finalUserVOMap = userVOMap;
        List<AiChatRecordVO> voList = records.stream().map(record -> {
            AiChatRecordVO vo = AiChatRecordConvert.INSTANCE.objToVo(record);
            vo.setUserVO(finalUserVOMap.get(record.getUserId()));
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }
}
