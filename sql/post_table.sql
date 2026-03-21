-- ============================================
-- 帖子表
-- ============================================

USE algorithm_cloud;

DROP TABLE IF EXISTS `post`;

CREATE TABLE `post`
(
    `id`             bigint        NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
    `title`          varchar(512)  NOT NULL COMMENT '标题',
    `content`        text          NOT NULL COMMENT '内容',
    `cover`          varchar(1024) NOT NULL DEFAULT '' COMMENT '封面',
    `tags`           varchar(1024) NOT NULL DEFAULT '[]' COMMENT '标签列表（JSON）',
    `thumb_num`      int           NOT NULL DEFAULT 0 COMMENT '点赞数',
    `favour_num`     int           NOT NULL DEFAULT 0 COMMENT '收藏数',
    `user_id`        bigint        NOT NULL COMMENT '创建用户ID',
    `content_type`   tinyint       NOT NULL DEFAULT 0 COMMENT '内容类型(0-帖子 1-算法知识库)',
    `review_status`  tinyint       NOT NULL DEFAULT 0 COMMENT '审核状态(0-待审核 1-通过 2-拒绝)',
    `review_message` varchar(512)  NOT NULL DEFAULT '' COMMENT '审核信息',
    `create_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`      tinyint       NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`) COMMENT '用户ID索引',
    KEY `idx_create_time` (`create_time`) COMMENT '创建时间索引',
    KEY `idx_review_status` (`review_status`) COMMENT '审核状态索引',
    KEY `idx_is_delete_create_time` (`is_delete`, `create_time` DESC) COMMENT '未删除帖子按时间倒序索引',
    KEY `idx_is_delete_content_type_create_time` (`is_delete`, `content_type`, `create_time` DESC) COMMENT '按类型与时间列表',
    KEY `idx_user_id_is_delete` (`user_id`, `is_delete`) COMMENT '用户未删除帖子索引',
    KEY `idx_is_delete_thumb_num` (`is_delete`, `thumb_num` DESC) COMMENT '热门帖子按点赞数倒序索引',
    KEY `idx_is_delete_favour_num` (`is_delete`, `favour_num` DESC) COMMENT '热门帖子按收藏数倒序索引',
    KEY `idx_status_delete_update` (`review_status`, `is_delete`, `update_time`) COMMENT '审核状态同步索引',
    FULLTEXT KEY `ft_title_content` (`title`, `content`) COMMENT '标题内容全文索引'
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '帖子表';
