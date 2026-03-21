-- ============================================
-- 邮件记录表（支持分布式事务补偿）
-- ============================================

USE algorithm_cloud;

DROP TABLE IF EXISTS `email_record`;

CREATE TABLE `email_record`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `msg_id`          varchar(128)          DEFAULT NULL COMMENT '消息ID',
    `biz_id`          varchar(128)          DEFAULT NULL COMMENT '业务幂等ID',
    `biz_type`        varchar(64)           DEFAULT NULL COMMENT '业务类型',
    `to_email`        varchar(256) NOT NULL COMMENT '收件人邮箱',
    `subject`         varchar(256) NOT NULL COMMENT '邮件主题',
    `content`         text COMMENT '邮件内容',
    `is_html`         tinyint      NOT NULL DEFAULT 0 COMMENT '是否HTML',
    `status`          varchar(32)  NOT NULL DEFAULT 'PENDING' COMMENT '发送状态: PENDING-待发送, SUCCESS-发送成功, FAILED-发送失败, CANCELLED-业务取消',
    `retry_count`     int          NOT NULL DEFAULT 0 COMMENT '重试次数',
    `max_retry`       int          NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    `error_message`   varchar(1024)         DEFAULT NULL COMMENT '错误信息',
    `provider`        varchar(64)           DEFAULT NULL COMMENT '发送渠道',
    `send_time`       datetime              DEFAULT NULL COMMENT '发送时间',
    `next_retry_time` datetime              DEFAULT NULL COMMENT '下次重试时间',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`       tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_msg_id` (`msg_id`) COMMENT '消息ID唯一索引',
    KEY `idx_to_email` (`to_email`) COMMENT '收件人索引',
    KEY `idx_biz_id` (`biz_id`) COMMENT '业务幂等索引',
    KEY `idx_status_create_time` (`status`, `create_time` DESC) COMMENT '状态时间索引',
    KEY `idx_pending_retry` (`status`, `next_retry_time`) COMMENT '待重试邮件索引',
    KEY `idx_biz_type` (`biz_type`) COMMENT '业务类型索引'
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '邮件记录表';
