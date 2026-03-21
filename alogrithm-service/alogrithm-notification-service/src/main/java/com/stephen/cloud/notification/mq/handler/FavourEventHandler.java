package com.stephen.cloud.notification.mq.handler;

import com.stephen.cloud.api.notification.model.enums.NotificationRelatedTypeEnum;
import com.stephen.cloud.api.notification.model.enums.NotificationStatusEnum;
import com.stephen.cloud.api.notification.model.enums.NotificationTypeEnum;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqDedupeLock;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.event.FavourEvent;
import com.stephen.cloud.notification.model.entity.Notification;
import com.stephen.cloud.notification.service.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RabbitMqDedupeLock(prefix = "mq:notification:favour", expire = 86400)
public class FavourEventHandler implements RabbitMqHandler<FavourEvent> {

    @Resource
    private NotificationService notificationService;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.FAVOUR_EVENT.getValue();
    }

    @Override
    public void onMessage(FavourEvent event, RabbitMessage rabbitMessage) throws Exception {
        if (event.getFavourId() == null || event.getPostAuthorId() == null) {
            log.error("[FavourEventHandler] 收藏事件解析失败或缺少必要字段, msgId: {}", rabbitMessage.getMsgId());
            throw new IllegalArgumentException("缺少必要字段");
        }

        log.info("[FavourEventHandler] 收到收藏事件, favourId: {}, postAuthorId: {}",
                event.getFavourId(), event.getPostAuthorId());

        Notification notification = new Notification();
        notification.setType(NotificationTypeEnum.FAVOUR.getCode());
        notification.setUserId(event.getPostAuthorId());
        notification.setTitle("收到新收藏");
        notification.setContent(String.format("%s 收藏了你的帖子《%s》",
                event.getFavourUserName(), event.getPostTitle()));
        notification.setRelatedId(event.getPostId());
        notification.setRelatedType(NotificationRelatedTypeEnum.POST.getValue());
        notification.setBizId("favour_" + event.getFavourId());
        notification.setIsRead(NotificationStatusEnum.UNREAD.getValue());

        notificationService.addNotification(notification);

        log.info("[FavourEventHandler] 收藏通知创建成功, favourId: {}, notificationId: {}",
                event.getFavourId(), notification.getId());
    }

    @Override
    public Class<FavourEvent> getDataType() {
        return FavourEvent.class;
    }
}
