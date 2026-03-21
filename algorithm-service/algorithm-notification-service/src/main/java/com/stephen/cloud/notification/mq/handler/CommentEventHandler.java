package com.stephen.cloud.notification.mq.handler;

import com.stephen.cloud.api.notification.model.enums.NotificationRelatedTypeEnum;
import com.stephen.cloud.api.notification.model.enums.NotificationStatusEnum;
import com.stephen.cloud.api.notification.model.enums.NotificationTypeEnum;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqDedupeLock;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.event.CommentEvent;
import com.stephen.cloud.notification.model.entity.Notification;
import com.stephen.cloud.notification.service.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 评论事件处理器
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
@RabbitMqDedupeLock(prefix = "mq:notification:comment", expire = 86400)
public class CommentEventHandler implements RabbitMqHandler<CommentEvent> {

    @Resource
    private NotificationService notificationService;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.COMMENT_EVENT.getValue();
    }

    @Override
    public void onMessage(CommentEvent event, RabbitMessage rabbitMessage) throws Exception {
        if (event.getCommentId() == null || event.getPostAuthorId() == null) {
            log.error("[CommentEventHandler] 评论事件解析失败或缺少必要字段, msgId: {}", rabbitMessage.getMsgId());
            throw new IllegalArgumentException("缺少必要字段");
        }

        log.info("[CommentEventHandler] 收到评论事件, commentId: {}, postAuthorId: {}",
                event.getCommentId(), event.getPostAuthorId());

        // 创建评论通知
        Notification notification = new Notification();
        notification.setType(NotificationTypeEnum.COMMENT.getCode());
        notification.setUserId(event.getPostAuthorId());
        notification.setTitle("收到新评论");
        notification.setContent(String.format("%s 评论了你的帖子《%s》：%s",
                event.getCommentAuthorName(), event.getPostTitle(), event.getCommentContent()));
        notification.setRelatedId(event.getCommentId());
        notification.setRelatedType(NotificationRelatedTypeEnum.COMMENT.getValue());
        notification.setBizId("comment_" + event.getCommentId());
        notification.setIsRead(NotificationStatusEnum.UNREAD.getValue());

        notificationService.addNotification(notification);

        log.info("[CommentEventHandler] 评论通知创建成功, commentId: {}, notificationId: {}",
                event.getCommentId(), notification.getId());
    }

    @Override
    public Class<CommentEvent> getDataType() {
        return CommentEvent.class;
    }
}
