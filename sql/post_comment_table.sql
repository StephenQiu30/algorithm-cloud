-- ============================================
-- 帖子评论表
-- ============================================

USE algorithm_cloud;

DROP TABLE IF EXISTS `post_comment`;

CREATE TABLE `post_comment`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `content`     text     NOT NULL COMMENT '评论内容',
    `post_id`     bigint   NOT NULL COMMENT '帖子ID',
    `user_id`     bigint   NOT NULL COMMENT '评论用户ID',
    `parent_id`   bigint   NOT NULL DEFAULT 0 COMMENT '父评论ID（0表示一级评论）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint  NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY           `idx_post_id` (`post_id`) COMMENT '帖子ID索引',
    KEY           `idx_user_id` (`user_id`) COMMENT '用户ID索引',
    KEY           `idx_parent_id` (`parent_id`) COMMENT '父评论ID索引',
    KEY           `idx_post_id_is_delete_create_time` (`post_id`, `is_delete`, `create_time`) COMMENT '帖子未删除评论按时间索引',
    KEY           `idx_post_id_parent_id_is_delete` (`post_id`, `parent_id`, `is_delete`) COMMENT '评论树形结构查询优化索引',
    KEY           `idx_post_id_create_time` (`post_id`, `create_time` DESC) COMMENT '最新评论查询优化索引'
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '帖子评论表';
