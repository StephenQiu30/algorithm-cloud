package com.stephen.cloud.notification.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.notification.model.dto.*;
import com.stephen.cloud.api.notification.model.vo.NotificationVO;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.DeleteRequest;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.log.annotation.OperationLog;
import com.stephen.cloud.notification.convert.NotificationConvert;
import com.stephen.cloud.notification.model.entity.Notification;
import com.stephen.cloud.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知接口
 * <p>
 * 提供通知的增删改查、标记已读、未读计数等功能。
 * 支持管理员智能创建通知，支持分页获取“我的通知”。
 * </p>
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/notification")
@Slf4j
@Tag(name = "NotificationController", description = "通知管理接口")
public class NotificationController {

    /**
     * 通知业务服务，处理生命周期、分发及推送逻辑
     */
    @Resource
    private NotificationService notificationService;

    /**
     * 创建通知 (管理员智能创建入口)
     * <p>
     * 管理员可以通过此接口快速向特定目标（全员、指定角色、指定用户列表）发送通知。
     * </p>
     *
     * @param notificationAddRequest 创建请求
     * @return 成功的通知 ID 列表
     */
    @PostMapping("/add")
    @OperationLog(module = "通知管理", action = "智能创建通知")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "创建通知", description = "管理员向特定目标发送通知")
    public BaseResponse<List<Long>> addNotification(@RequestBody NotificationAddRequest notificationAddRequest) {
        ThrowUtils.throwIf(notificationAddRequest == null, ErrorCode.PARAMS_ERROR);
        List<Long> ids = notificationService.addNotification(notificationAddRequest);
        return ResultUtils.success(ids);
    }

    /**
     * 删除通知
     *
     * @param deleteRequest 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @OperationLog(module = "通知管理", action = "删除通知")
    @Operation(summary = "删除通知", description = "删除指定通知，仅本人或管理员可操作")
    public BaseResponse<Boolean> deleteNotification(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        long id = deleteRequest.getId();
        // 判断是否存在
        Notification notification = notificationService.getById(id);
        ThrowUtils.throwIf(notification == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        ThrowUtils.throwIf(!notification.getUserId().equals(userId) && !SecurityUtils.isAdmin(),
                ErrorCode.NO_AUTH_ERROR);
        boolean result = notificationService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 更新通知（管理员）
     *
     * @param notificationUpdateRequest 更新请求
     * @return 是否成功
     */
    @PostMapping("/update")
    @OperationLog(module = "通知管理", action = "更新通知")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "更新通知", description = "更新指定通知，仅管理员可用")
    public BaseResponse<Boolean> updateNotification(@RequestBody NotificationUpdateRequest notificationUpdateRequest) {
        ThrowUtils.throwIf(notificationUpdateRequest == null || notificationUpdateRequest.getId() == null,
                ErrorCode.PARAMS_ERROR);
        long id = notificationUpdateRequest.getId();
        // 判断是否存在
        Notification oldNotification = notificationService.getById(id);
        ThrowUtils.throwIf(oldNotification == null, ErrorCode.NOT_FOUND_ERROR);

        Notification notification = NotificationConvert.updateRequestToObj(notificationUpdateRequest);
        ThrowUtils.throwIf(notification == null, ErrorCode.PARAMS_ERROR);
        notificationService.validNotification(notification, false);

        boolean result = notificationService.updateById(notification);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(result);
    }

    /**
     * 根据 ID 获取通知（视图对象）
     *
     * @param id 通知 ID
     * @return 通知信息
     */
    @GetMapping("/get/vo")
    @Operation(summary = "获取通知详情", description = "根据 ID 获取通知脱敏后的视图对象")
    public BaseResponse<NotificationVO> getNotificationVOById(@RequestParam("id") long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Notification notification = notificationService.getById(id);
        ThrowUtils.throwIf(notification == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可查看
        Long loginUserId = SecurityUtils.getLoginUserId();
        ThrowUtils.throwIf(!notification.getUserId().equals(loginUserId) && !SecurityUtils.isAdmin(),
                ErrorCode.NO_AUTH_ERROR);
        return ResultUtils.success(notificationService.getNotificationVO(notification));
    }

    /**
     * 分页获取通知列表（用于同步）
     *
     * @param notificationQueryRequest 查询请求
     * @return 通知分页列表
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "分页获取通知列表（用于同步）", description = "获取系统通知的完整记录分页列表，仅限管理员权限。")
    public BaseResponse<Page<Notification>> listNotificationByPage(
            @RequestBody NotificationQueryRequest notificationQueryRequest) {
        ThrowUtils.throwIf(notificationQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = notificationQueryRequest.getCurrent();
        long size = notificationQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Notification> notificationPage = notificationService.page(new Page<>(current, size),
                notificationService.getQueryWrapper(notificationQueryRequest));
        return ResultUtils.success(notificationPage);
    }

    /**
     * 分页获取通知列表（封装类）
     *
     * @param notificationQueryRequest 查询请求
     * @return 通知 VO 分页列表
     */
    @PostMapping("/list/page/vo")
    @Operation(summary = "分页获取通知列表（封装类）", description = "以脱敏视图形式分页获取通知列表，普通用户仅能查看自己的数据。")
    public BaseResponse<Page<NotificationVO>> listNotificationVOByPage(
            @RequestBody NotificationQueryRequest notificationQueryRequest) {
        ThrowUtils.throwIf(notificationQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = notificationQueryRequest.getCurrent();
        long size = notificationQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        // 如果不是管理员，强制只能查看自己的通知
        if (!SecurityUtils.isAdmin()) {
            notificationQueryRequest.setUserId(SecurityUtils.getLoginUserId());
        }

        Page<Notification> notificationPage = notificationService.page(new Page<>(current, size),
                notificationService.getQueryWrapper(notificationQueryRequest));
        return ResultUtils.success(notificationService.getNotificationVOPage(notificationPage));
    }

    /**
     * 我的通知列表
     *
     * @param notificationQueryRequest 查询请求
     * @return 用户的通知 VO 分页列表
     */
    @PostMapping("/my/list/page/vo")
    @Operation(summary = "获取当前用户的通知列表", description = "分页检索当前登录用户收到的所有通知，包含关联的用户信息。")
    public BaseResponse<Page<NotificationVO>> listMyNotificationVOByPage(
            @RequestBody NotificationQueryRequest notificationQueryRequest) {
        ThrowUtils.throwIf(notificationQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long userId = SecurityUtils.getLoginUserId();
        notificationQueryRequest.setUserId(userId);
        long current = notificationQueryRequest.getCurrent();
        long size = notificationQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Notification> notificationPage = notificationService.page(new Page<>(current, size),
                notificationService.getQueryWrapper(notificationQueryRequest));
        return ResultUtils.success(notificationService.getNotificationVOPage(notificationPage));
    }

    /**
     * 标记通知已读
     *
     * @param notificationReadRequest 标记已读请求
     * @return 是否成功
     */
    @PostMapping("/read")
    @OperationLog(module = "通知管理", action = "标记已读")
    @Operation(summary = "标记已读", description = "将指定通知标记为已读状态")
    public BaseResponse<Boolean> markNotificationRead(@RequestBody NotificationReadRequest notificationReadRequest) {
        ThrowUtils.throwIf(notificationReadRequest == null || notificationReadRequest.getId() == null
                || notificationReadRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        boolean result = notificationService.markRead(notificationReadRequest.getId(), SecurityUtils.getLoginUserId(),
                SecurityUtils.isAdmin());
        return ResultUtils.success(result);
    }

    /**
     * 标记全部通知已读
     *
     * @return 是否成功
     */
    @PostMapping("/read/all")
    @OperationLog(module = "通知管理", action = "全部标记已读")
    @Operation(summary = "全部标记已读", description = "将当前用户的所有未读通知标记为已读")
    public BaseResponse<Boolean> markAllNotificationRead() {
        boolean result = notificationService.markAllRead(SecurityUtils.getLoginUserId());
        return ResultUtils.success(result);
    }

    /**
     * 获取未读通知数量
     *
     * @return 未读数量
     */
    @GetMapping("/unread/count")
    @Operation(summary = "获取未读通知数", description = "获取当前用户未读通知的总数")
    public BaseResponse<Long> getNotificationUnreadCount() {
        long count = notificationService.getUnreadCount(SecurityUtils.getLoginUserId());
        return ResultUtils.success(count);
    }

    /**
     * 批量删除通知
     *
     * @param batchDeleteRequest 批量删除请求
     * @return 删除成功数量
     */
    @PostMapping("/batch/delete")
    @OperationLog(module = "通知管理", action = "批量删除通知")
    @Operation(summary = "批量删除通知", description = "批量删除选中的通知")
    public BaseResponse<Integer> batchDeleteNotification(
            @RequestBody NotificationBatchDeleteRequest batchDeleteRequest) {
        ThrowUtils.throwIf(batchDeleteRequest == null || CollUtil.isEmpty(batchDeleteRequest.getIds()),
                ErrorCode.PARAMS_ERROR);
        int count = notificationService.batchDeleteNotification(batchDeleteRequest.getIds(),
                SecurityUtils.getLoginUserId(), SecurityUtils.isAdmin());
        return ResultUtils.success(count);
    }

    /**
     * 批量标记已读
     *
     * @param batchReadRequest 批量标记已读请求
     * @return 标记成功数量
     */
    @PostMapping("/batch/read")
    @OperationLog(module = "通知管理", action = "批量标记已读")
    @Operation(summary = "批量标记已读", description = "批量将选中通知标记为已读")
    public BaseResponse<Integer> batchMarkNotificationRead(@RequestBody NotificationBatchReadRequest batchReadRequest) {
        ThrowUtils.throwIf(batchReadRequest == null || CollUtil.isEmpty(batchReadRequest.getIds()),
                ErrorCode.PARAMS_ERROR);
        int count = notificationService.batchMarkRead(batchReadRequest.getIds(), SecurityUtils.getLoginUserId(),
                SecurityUtils.isAdmin());
        return ResultUtils.success(count);
    }
}
