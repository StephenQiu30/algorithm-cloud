package com.stephen.cloud.notification.mq.handler;

import com.stephen.cloud.api.notification.model.enums.NotificationRelatedTypeEnum;
import com.stephen.cloud.api.notification.model.enums.NotificationStatusEnum;
import com.stephen.cloud.api.notification.model.enums.NotificationTypeEnum;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqDedupeLock;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import com.stephen.cloud.common.rabbitmq.model.event.PostReviewEvent;
import com.stephen.cloud.notification.model.entity.Notification;
import com.stephen.cloud.notification.service.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 帖子审核结果事件处理器
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
@RabbitMqDedupeLock(prefix = "mq:notification:post_review", expire = 86400)
public class PostReviewEventHandler implements RabbitMqHandler<PostReviewEvent> {

    @Resource
    private NotificationService notificationService;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.POST_REVIEW_EVENT.getValue();
    }

    @Override
    public void onMessage(PostReviewEvent event, RabbitMessage rabbitMessage) throws Exception {
        if (event.getPostId() == null || event.getAuthorId() == null) {
            log.error("[PostReviewEventHandler] 审核事件解析失败或缺少必要字段, msgId: {}", rabbitMessage.getMsgId());
            throw new IllegalArgumentException("缺少必要字段");
        }

        log.info("[PostReviewEventHandler] 收到帖子审核结果事件, postId: {}, authorId: {}",
                event.getPostId(), event.getAuthorId());

        Notification notification = new Notification();
        notification.setType(NotificationTypeEnum.POST_REVIEW.getCode());
        notification.setUserId(event.getAuthorId());
        String statusText = event.getStatus() == 1 ? "通过" : "被拒绝";
        notification.setTitle("帖子审核结果");
        notification.setContent(String.format("你的帖子《%s》审核%s。%s",
                event.getPostTitle(), statusText,
                StringUtils.isNotBlank(event.getMessage()) ? "意见：" + event.getMessage() : ""));

        notification.setRelatedId(event.getPostId());
        notification.setRelatedType(NotificationRelatedTypeEnum.POST.getValue());
        notification.setBizId("post_review_" + event.getPostId() + "_" + event.getStatus());
        notification.setIsRead(NotificationStatusEnum.UNREAD.getValue());

        notificationService.addNotification(notification);

        log.info("[PostReviewEventHandler] 审核通知创建成功, postId: {}, notificationId: {}",
                event.getPostId(), notification.getId());
    }

    @Override
    public Class<PostReviewEvent> getDataType() {
        return PostReviewEvent.class;
    }
}
