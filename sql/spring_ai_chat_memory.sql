-- ============================================
-- Spring AI 对话记忆持久化表 (JDBC 实现)
-- ============================================

USE algorithm;

CREATE TABLE IF NOT EXISTS `SPRING_AI_CHAT_MEMORY` (
    `conversation_id` varchar(128) NOT NULL COMMENT '会话ID',
    `content`         text         NOT NULL COMMENT '消息文本内容',
    `type`            enum('USER', 'ASSISTANT', 'SYSTEM', 'TOOL') NOT NULL COMMENT '消息角色类型',
    `timestamp`       timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息创建时间',
    KEY `idx_conversation_timestamp` (`conversation_id`, `timestamp`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Spring AI 标准对话记忆表';
