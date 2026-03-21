-- ============================================
-- 帖子点赞表
-- ============================================

USE alogrithm_cloud;

DROP TABLE IF EXISTS `post_thumb`;

CREATE TABLE `post_thumb`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
    `post_id`     bigint   NOT NULL COMMENT '帖子ID',
    `user_id`     bigint   NOT NULL COMMENT '用户ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_user` (`post_id`, `user_id`) COMMENT '用户对帖子点赞唯一索引',
    KEY           `idx_post_id` (`post_id`) COMMENT '帖子ID索引',
    KEY           `idx_user_id` (`user_id`) COMMENT '用户ID索引',
    KEY           `idx_user_id_create_time` (`user_id`, `create_time` DESC) COMMENT '用户点赞历史查询优化索引'
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '帖子点赞表';
