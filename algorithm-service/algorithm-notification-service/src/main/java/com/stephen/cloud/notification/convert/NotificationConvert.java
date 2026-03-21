package com.stephen.cloud.notification.convert;

import com.stephen.cloud.api.notification.model.dto.NotificationAddRequest;
import com.stephen.cloud.api.notification.model.dto.NotificationUpdateRequest;
import com.stephen.cloud.api.notification.model.vo.NotificationVO;
import com.stephen.cloud.notification.model.entity.Notification;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知转换器
 *
 * @author StephenQiu30
 */
public class NotificationConvert {

    /**
     * 对象转视图
     *
     * @param notification 通知
     * @return 通知视图对象
     */
    public static NotificationVO objToVo(Notification notification) {
        if (notification == null) {
            return null;
        }
        NotificationVO notificationVO = new NotificationVO();
        BeanUtils.copyProperties(notification, notificationVO);
        return notificationVO;
    }

    /**
     * 对象列表转视图列表
     *
     * @param notificationList 通知列表
     * @return 通知视图对象列表
     */
    public static List<NotificationVO> getNotificationVO(List<Notification> notificationList) {
        if (notificationList == null || notificationList.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return notificationList.stream().map(NotificationConvert::objToVo).collect(Collectors.toList());
    }

    /**
     * 新增请求转对象
     *
     * @param notificationAddRequest 新增请求
     * @return 通知实体
     */
    public static Notification addRequestToObj(NotificationAddRequest notificationAddRequest) {
        if (notificationAddRequest == null) {
            return null;
        }
        Notification notification = new Notification();
        BeanUtils.copyProperties(notificationAddRequest, notification);
        return notification;
    }

    /**
     * 更新请求转对象
     *
     * @param notificationUpdateRequest 更新请求
     * @return 通知实体
     */
    public static Notification updateRequestToObj(NotificationUpdateRequest notificationUpdateRequest) {
        if (notificationUpdateRequest == null) {
            return null;
        }
        Notification notification = new Notification();
        BeanUtils.copyProperties(notificationUpdateRequest, notification);
        return notification;
    }
}
