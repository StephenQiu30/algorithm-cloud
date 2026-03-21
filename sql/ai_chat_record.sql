-- ============================================
-- AI 对话记录表
-- ============================================

USE
stephen_ai;

DROP TABLE IF EXISTS `ai_chat_record`;

CREATE TABLE `ai_chat_record`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '对话ID',
    `user_id`     bigint   NOT NULL COMMENT '用户ID',
    `session_id`  varchar(128)      DEFAULT NULL COMMENT '会话ID',
    `message`     text     NOT NULL COMMENT '对话消息',
    `response`    text              DEFAULT NULL COMMENT 'AI响应内容',
    `model_type`        varchar(128)      DEFAULT NULL COMMENT '模型类型',
    `total_tokens`      int               DEFAULT NULL COMMENT '总消耗 token',
    `prompt_tokens`     int               DEFAULT NULL COMMENT '提示消耗 token',
    `completion_tokens` int               DEFAULT NULL COMMENT '生成消耗 token',
    `post_id`           bigint             DEFAULT NULL COMMENT '关联帖子ID',
    `create_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint  NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY           `idx_user_id` (`user_id`) COMMENT '用户ID索引',
    KEY           `idx_session_id` (`session_id`) COMMENT '会话ID索引',
    KEY           `idx_create_time` (`create_time`) COMMENT '创建时间索引',
    KEY           `idx_user_id_create_time` (`user_id`, `create_time` DESC) COMMENT '用户对话历史按时间倒序索引',
    KEY           `idx_session_create` (`session_id`, `create_time` DESC) COMMENT '会话聊天历史索引'
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'AI 对话记录表';
