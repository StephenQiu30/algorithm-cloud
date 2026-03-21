package com.stephen.cloud.notification.mq.handler;

import com.stephen.cloud.api.notification.model.enums.NotificationRelatedTypeEnum;
import com.stephen.cloud.api.notification.model.enums.NotificationStatusEnum;
import com.stephen.cloud.api.notification.model.enums.NotificationTypeEnum;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqDedupeLock;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.event.LikeEvent;
import com.stephen.cloud.notification.model.entity.Notification;
import com.stephen.cloud.notification.service.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 点赞事件处理器
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
@RabbitMqDedupeLock(prefix = "mq:notification:like", expire = 86400)
public class LikeEventHandler implements RabbitMqHandler<LikeEvent> {

    @Resource
    private NotificationService notificationService;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.LIKE_EVENT.getValue();
    }

    @Override
    public void onMessage(LikeEvent event, RabbitMessage rabbitMessage) throws Exception {
        if (event.getLikeId() == null || event.getPostAuthorId() == null) {
            log.error("[LikeEventHandler] 点赞事件解析失败" +
                    "或缺少必要字段, msgId: {}", rabbitMessage.getMsgId());
            throw new IllegalArgumentException("缺少必要字段");
        }

        log.info("[LikeEventHandler] 收到点赞事件, likeId: {}, postAuthorId: {}",
                event.getLikeId(), event.getPostAuthorId());

        // 创建点赞通知
        Notification notification = new Notification();
        notification.setType(NotificationTypeEnum.LIKE.getCode());
        notification.setUserId(event.getPostAuthorId());
        notification.setTitle("收到新点赞");
        notification.setContent(String.format("%s 赞了你的帖子《%s》",
                event.getLikeUserName(), event.getPostTitle()));
        notification.setRelatedId(event.getPostId());
        notification.setRelatedType(NotificationRelatedTypeEnum.POST.getValue());
        notification.setBizId("like_" + event.getLikeId());
        notification.setIsRead(NotificationStatusEnum.UNREAD.getValue());

        notificationService.addNotification(notification);

        log.info("[LikeEventHandler] 点赞通知创建成功, likeId: {}, notificationId: {}",
                event.getLikeId(), notification.getId());
    }

    @Override
    public Class<LikeEvent> getDataType() {
        return LikeEvent.class;
    }
}
