USE algorithm_cloud;

ALTER TABLE `post`
    ADD COLUMN `content_type` tinyint NOT NULL DEFAULT 0 COMMENT '内容类型(0-帖子 1-算法知识库)' AFTER `user_id`,
    ADD KEY `idx_is_delete_content_type_create_time` (`is_delete`, `content_type`, `create_time` DESC);
