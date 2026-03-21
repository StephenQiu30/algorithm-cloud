package com.stephen.cloud.notification.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.notification.model.dto.NotificationAddRequest;
import com.stephen.cloud.api.notification.model.dto.NotificationQueryRequest;
import com.stephen.cloud.api.notification.model.enums.NotificationStatusEnum;
import com.stephen.cloud.api.notification.model.enums.NotificationTargetTypeEnum;
import com.stephen.cloud.api.notification.model.enums.NotificationTypeEnum;
import com.stephen.cloud.api.notification.model.vo.NotificationVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.dto.UserQueryRequest;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import com.stephen.cloud.notification.convert.NotificationConvert;
import com.stephen.cloud.notification.mapper.NotificationMapper;
import com.stephen.cloud.notification.model.entity.Notification;
import com.stephen.cloud.notification.mq.NotificationMqProducer;
import com.stephen.cloud.notification.service.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知服务实现
 * <p>
 * MVP 简化版实现规范：
 * 1. <b>显式关联</b>：移除自动正则 URL 解析，要求调用方明确提供相关业务 ID。
 * 2. <b>稳健性优先</b>：跨服务数据装配采用简单的批量串行逻辑，降低并发调试成本。
 * 3. <b>性能平衡</b>：保留 {@code saveBatch} 与 {@code afterCommit} 推送，确保生产级的吞吐量与一致性。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
        implements NotificationService {

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private NotificationMqProducer notificationMqProducer;

    /**
     * 校验通知合法性
     *
     * @param notification 通知正文
     * @param add          是否为新增
     */
    @Override
    public void validNotification(Notification notification, boolean add) {
        if (notification == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = notification.getTitle();
        String content = notification.getContent();
        if (add) {
            if (StringUtils.isAnyBlank(title, content)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题或内容不能为空");
            }
        }
        if (StringUtils.isNotBlank(title) && title.length() > 256) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
        if (StringUtils.isNotBlank(content) && content.length() > 2048) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容过长");
        }
    }

    /**
     * 构建 MyBatis-Plus 查询条件
     *
     * @param notificationQueryRequest 查询请求包
     * @return LambdaQueryWrapper
     */
    @Override
    public LambdaQueryWrapper<Notification> getQueryWrapper(NotificationQueryRequest notificationQueryRequest) {
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        if (notificationQueryRequest == null) {
            return queryWrapper;
        }

        Long id = notificationQueryRequest.getId();
        String type = notificationQueryRequest.getType();
        Long userId = notificationQueryRequest.getUserId();
        String relatedType = notificationQueryRequest.getRelatedType();
        Integer isRead = notificationQueryRequest.getIsRead();
        Integer status = notificationQueryRequest.getStatus();
        String searchText = notificationQueryRequest.getSearchText();
        String sortField = notificationQueryRequest.getSortField();
        String sortOrder = notificationQueryRequest.getSortOrder();

        // 精确匹配
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), Notification::getId, id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), Notification::getUserId, userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(type), Notification::getType, type);
        queryWrapper.eq(ObjectUtils.isNotEmpty(relatedType), Notification::getRelatedType, relatedType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(isRead), Notification::getIsRead, isRead);
        queryWrapper.eq(ObjectUtils.isNotEmpty(status), Notification::getStatus, status);

        // 模糊搜索：匹配标题或内容
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like(Notification::getTitle, searchText)
                    .or()
                    .like(Notification::getContent, searchText));
        }

        // 排序逻辑
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                Notification::getCreateTime);

        return queryWrapper;
    }

    /**
     * 获取通知脱敏视图对象（单条）
     *
     * @param notification 数据库记录
     * @return NotificationVO
     */
    @Override
    public NotificationVO getNotificationVO(Notification notification) {
        if (notification == null) {
            return null;
        }
        NotificationVO notificationVO = NotificationConvert.objToVo(notification);
        if (notificationVO != null && notification.getUserId() != null && notification.getUserId() > 0) {
            try {
                BaseResponse<UserVO> userResponse = userFeignClient.getUserVOById(notification.getUserId());
                if (userResponse != null && userResponse.getData() != null) {
                    notificationVO.setUserVO(userResponse.getData());
                }
            } catch (Exception e) {
                log.error("[NotificationServiceImpl] 获取单条通知用户信息失败", e);
            }
        }
        return notificationVO;
    }

    /**
     * 分页转换通知视图对象组
     * <p>
     * MVP 简化版：采用批量串行查询，降低代码复杂度，确保系统稳健。
     * </p>
     *
     * @param notificationPage 原始结果页
     * @return VO 结果页
     */
    @Override
    public Page<NotificationVO> getNotificationVOPage(Page<Notification> notificationPage) {
        List<Notification> notificationList = notificationPage.getRecords();
        Page<NotificationVO> notificationVOPage = new Page<>(notificationPage.getCurrent(), notificationPage.getSize(),
                notificationPage.getTotal());
        if (CollUtil.isEmpty(notificationList)) {
            return notificationVOPage;
        }

        // 批量提取不重复的用户 ID，排除全员通知(0)
        Set<Long> userIdSet = notificationList.stream()
                .map(Notification::getUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        Map<Long, UserVO> userVOMap = new HashMap<>();
        if (CollUtil.isNotEmpty(userIdSet)) {
            try {
                BaseResponse<List<UserVO>> userResponse = userFeignClient.getUserVOByIds(new ArrayList<>(userIdSet));
                if (userResponse != null && CollUtil.isNotEmpty(userResponse.getData())) {
                    userVOMap = userResponse.getData().stream()
                            .collect(Collectors.toMap(UserVO::getId, userVO -> userVO, (a, b) -> a));
                }
            } catch (Exception e) {
                log.error("[NotificationServiceImpl] 批量获取用户信息失败", e);
            }
        }

        // 属性拷贝并填充用户信息
        final Map<Long, UserVO> finalUserVOMap = userVOMap;
        List<NotificationVO> notificationVOList = notificationList.stream().map(notification -> {
            NotificationVO notificationVO = NotificationConvert.objToVo(notification);
            if (notificationVO != null) {
                notificationVO.setUserVO(finalUserVOMap.get(notification.getUserId()));
            }
            return notificationVO;
        }).collect(Collectors.toList());

        notificationVOPage.setRecords(notificationVOList);
        return notificationVOPage;
    }

    /**
     * 基础通知创建 (点对点)
     * <p>
     * 流程包含：参数校验、幂等 ID 生成、落地存储、以及事务提交后的异步 MQ 推送。
     * </p>
     *
     * @param notification 实体
     * @return 通知 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addNotification(Notification notification) {
        validNotification(notification, true);

        // 默认幂等 ID 处理
        String bizId = notification.getBizId();
        if (StringUtils.isBlank(bizId)) {
            bizId = "manual_" + IdUtil.fastSimpleUUID();
            notification.setBizId(bizId);
        }

        Long userId = notification.getUserId();
        Notification existing = this.lambdaQuery()
                .eq(Notification::getBizId, bizId)
                .eq(Notification::getUserId, userId)
                .one();
        if (existing != null) {
            return existing.getId();
        }

        if (notification.getIsRead() == null) {
            notification.setIsRead(NotificationStatusEnum.UNREAD.getValue());
        }

        try {
            boolean result = this.save(notification);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        } catch (DuplicateKeyException e) {
            // 数据库二级防御
            Notification again = this.lambdaQuery()
                    .eq(Notification::getBizId, bizId)
                    .eq(Notification::getUserId, userId)
                    .one();
            if (again != null) {
                return again.getId();
            }
            throw e;
        }

        // 发送推送逻辑（异步且保证事务一致性）
        registerPushHook(notification);

        return notification.getId();
    }

    /**
     * 注册事务推送钩子
     *
     * @param notification 通知实体
     */
    private void registerPushHook(Notification notification) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        NotificationVO notificationVO = NotificationConvert.objToVo(notification);
                        notificationMqProducer.sendNotificationCreated(notificationVO);
                    } catch (Exception e) {
                        log.error("[NotificationServiceImpl] 发送实时通知消息失败, notificationId: {}", notification.getId(), e);
                    }
                }
            });
        } else {
            try {
                NotificationVO notificationVO = NotificationConvert.objToVo(notification);
                notificationMqProducer.sendNotificationCreated(notificationVO);
            } catch (Exception e) {
                log.error("[NotificationServiceImpl] 非事务模式发送通知失败, notificationId: {}", notification.getId(), e);
            }
        }
    }

    /**
     * 管理员智能创建入口
     * <p>
     * 处理 target 维度（全员、指定列表、角色组）的分发，采用 {@code saveBatch} 提升分发效率。
     * </p>
     *
     * @param request 请求包
     * @return ID 序列
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> addNotification(NotificationAddRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String target = request.getTarget();
        String content = request.getContent();
        if (StringUtils.isAnyBlank(target, content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String finalTitle = StringUtils.isNotBlank(request.getTitle()) ? request.getTitle()
                : (content.length() > 20 ? content.substring(0, 20) + "..." : content);

        List<Long> targetUserIds = new ArrayList<>();
        boolean isBroadcast = false;

        // 解析分发目标
        if (NotificationTargetTypeEnum.ALL.getValue().equalsIgnoreCase(target)) {
            isBroadcast = true;
        } else if (target.startsWith(NotificationTargetTypeEnum.ROLE.getValue())) {
            String role = target.substring(NotificationTargetTypeEnum.ROLE.getValue().length());
            UserQueryRequest userQueryRequest = new UserQueryRequest();
            userQueryRequest.setUserRole(role);
            userQueryRequest.setPageSize(1000);
            try {
                BaseResponse<Page<UserVO>> userResponse = userFeignClient.listUserByPage(userQueryRequest);
                if (userResponse != null && userResponse.getData() != null) {
                    targetUserIds = userResponse.getData().getRecords().stream()
                            .map(UserVO::getId)
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.error("[NotificationServiceImpl] 获取角色用户组失败, role: {}", role, e);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取分发用户失败");
            }
        } else {
            try {
                targetUserIds = Arrays.stream(target.split(","))
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标 ID 序列格式错误");
            }
        }

        // 模板初始化
        Notification template = new Notification();
        template.setTitle(finalTitle);
        template.setContent(content);
        template.setContentUrl(request.getContentUrl());
        template.setType(NotificationTypeEnum.SYSTEM.getCode());
        template.setIsRead(NotificationStatusEnum.UNREAD.getValue());

        List<Notification> notificationsToSave = new ArrayList<>();
        if (isBroadcast) {
            Notification notification = new Notification();
            BeanUtils.copyProperties(template, notification);
            notification.setUserId(0L);
            notification.setBizId("broadcast_" + IdUtil.fastSimpleUUID());
            notificationsToSave.add(notification);
        } else if (CollUtil.isNotEmpty(targetUserIds)) {
            String batchPrefix = "batch_" + IdUtil.fastSimpleUUID() + "_";
            for (int i = 0; i < targetUserIds.size(); i++) {
                Notification notification = new Notification();
                BeanUtils.copyProperties(template, notification);
                notification.setUserId(targetUserIds.get(i));
                notification.setBizId(batchPrefix + i);
                notificationsToSave.add(notification);
            }
        }

        if (CollUtil.isEmpty(notificationsToSave)) {
            return Collections.emptyList();
        }

        boolean saveResult = this.saveBatch(notificationsToSave);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "分发保存失败");
        }

        // 事务提交后批量推送
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendBatchNotifications(notificationsToSave);
                }
            });
        } else {
            sendBatchNotifications(notificationsToSave);
        }

        return notificationsToSave.stream().map(Notification::getId).collect(Collectors.toList());
    }

    /**
     * 内部批量发送推送
     */
    private void sendBatchNotifications(List<Notification> notifications) {
        for (Notification notification : notifications) {
            try {
                NotificationVO notificationVO = NotificationConvert.objToVo(notification);
                notificationMqProducer.sendNotificationCreated(notificationVO);
            } catch (Exception e) {
                log.error("[NotificationServiceImpl] 批处理推送失败: {}", notification.getId());
            }
        }
    }

    /**
     * 标记单条通知已读
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markRead(Long notificationId, Long operatorId, boolean isAdmin) {
        Notification notification = this.getById(notificationId);
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (notification.getUserId() == null || (!notification.getUserId().equals(operatorId) && !isAdmin)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        notification.setIsRead(NotificationStatusEnum.READ.getValue());
        return this.updateById(notification);
    }

    /**
     * 标记用户全量已读
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAllRead(Long userId) {
        List<Notification> unreadList = this.lambdaQuery()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, NotificationStatusEnum.UNREAD.getValue())
                .list();
        if (CollUtil.isEmpty(unreadList)) {
            return true;
        }
        unreadList.forEach(item -> item.setIsRead(NotificationStatusEnum.READ.getValue()));
        return this.updateBatchById(unreadList);
    }

    /**
     * 获取用户未读数
     */
    @Override
    public long getUnreadCount(Long userId) {
        return this.lambdaQuery()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, NotificationStatusEnum.UNREAD.getValue())
                .count();
    }

    /**
     * 基础批量删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchDeleteNotification(List<Long> ids, Long operatorId, boolean isAdmin) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        List<Notification> list = this.listByIds(ids);
        if (CollUtil.isEmpty(list)) {
            return 0;
        }
        List<Long> deleteIds = list.stream()
                .filter(item -> isAdmin || item.getUserId().equals(operatorId))
                .map(Notification::getId)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(deleteIds)) {
            return 0;
        }
        boolean result = this.removeByIds(deleteIds);
        return result ? deleteIds.size() : 0;
    }

    /**
     * 基础批量标记已读
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchMarkRead(List<Long> ids, Long operatorId, boolean isAdmin) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        List<Notification> list = this.listByIds(ids);
        if (CollUtil.isEmpty(list)) {
            return 0;
        }
        List<Notification> updateList = list.stream()
                .filter(item -> item.getIsRead().equals(NotificationStatusEnum.UNREAD.getValue())
                        && (isAdmin || item.getUserId().equals(operatorId)))
                .peek(item -> item.setIsRead(NotificationStatusEnum.READ.getValue()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(updateList)) {
            return 0;
        }
        boolean result = this.updateBatchById(updateList);
        return result ? updateList.size() : 0;
    }
}
