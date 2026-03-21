package com.stephen.cloud.notification.mq.handler;

import com.stephen.cloud.api.notification.model.enums.NotificationRelatedTypeEnum;
import com.stephen.cloud.api.notification.model.enums.NotificationStatusEnum;
import com.stephen.cloud.api.notification.model.enums.NotificationTypeEnum;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqDedupeLock;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.event.FollowEvent;
import com.stephen.cloud.notification.model.entity.Notification;
import com.stephen.cloud.notification.service.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 关注事件处理器
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
@RabbitMqDedupeLock(prefix = "mq:notification:follow", expire = 86400)
public class FollowEventHandler implements RabbitMqHandler<FollowEvent> {

    @Resource
    private NotificationService notificationService;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.FOLLOW_EVENT.getValue();
    }

    @Override
    public void onMessage(FollowEvent event, RabbitMessage rabbitMessage) throws Exception {
        if (event.getFollowId() == null || event.getFollowedUserId() == null) {
            log.error("[FollowEventHandler] 关注事件解析失败" +
                    "或缺少必要字段, msgId: {}", rabbitMessage.getMsgId());
            throw new IllegalArgumentException("缺少必要字段");
        }

        log.info("[FollowEventHandler] 收到关注事件, followId: {}, followedUserId: {}",
                event.getFollowId(), event.getFollowedUserId());

        // 创建关注通知
        Notification notification = new Notification();
        notification.setType(NotificationTypeEnum.FOLLOW.getCode());
        notification.setUserId(event.getFollowedUserId());
        notification.setTitle("新粉丝");
        notification.setContent(String.format("%s 关注了你", event.getFollowerName()));
        notification.setRelatedId(event.getFollowerId());
        notification.setRelatedType(NotificationRelatedTypeEnum.USER.getValue());
        notification.setBizId("follow_" + event.getFollowId());
        notification.setIsRead(NotificationStatusEnum.UNREAD.getValue());

        notificationService.addNotification(notification);

        log.info("[FollowEventHandler] 关注通知创建成功" +
                ", followId: {}, notificationId: {}",
                event.getFollowId(), notification.getId());
    }

    @Override
    public Class<FollowEvent> getDataType() {
        return FollowEvent.class;
    }
}
