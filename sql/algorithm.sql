-- ============================================
-- 数据库初始化脚本
-- 包含模块：用户、内容、AI/RAG、系统日志
-- ============================================

-- 1. 数据库建立
CREATE DATABASE IF NOT EXISTS algorithm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE algorithm;

-- ============================================
-- 模块：用户系统
-- ============================================

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `user_name`       varchar(256)          DEFAULT NULL COMMENT '用户昵称',
    `user_avatar`     varchar(1024)         DEFAULT NULL COMMENT '用户头像',
    `user_profile`    varchar(512)          DEFAULT NULL COMMENT '用户简介',
    `user_role`       varchar(256) NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin/ban',
    `user_email`      varchar(256)          DEFAULT NULL COMMENT '用户邮箱',
    `email_verified`  tinyint               DEFAULT 0 COMMENT '邮箱是否验证：0-未验证，1-已验证',
    `user_phone`      varchar(128)          DEFAULT NULL COMMENT '用户手机号',
    `github_id`       varchar(256)          DEFAULT NULL COMMENT 'GitHub ID',
    `github_login`    varchar(256)          DEFAULT NULL COMMENT 'GitHub 账号',
    `github_url`      varchar(512)          DEFAULT NULL COMMENT 'GitHub 主页',
    `last_login_time` datetime              DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip`   varchar(128)          DEFAULT NULL COMMENT '最后登录IP',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`       tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_email` (`user_email`) COMMENT '用户邮箱唯一索引',
    KEY `idx_github_id` (`github_id`) COMMENT 'GitHub ID索引',
    KEY `idx_github_id_is_delete` (`github_id`, `is_delete`) COMMENT 'GitHub ID删除状态索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户表';

DROP TABLE IF EXISTS `user_login_log`;
CREATE TABLE `user_login_log`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '登录日志ID',
    `user_id`     bigint               DEFAULT NULL COMMENT '用户ID',
    `account`     varchar(256)         DEFAULT NULL COMMENT '登录账号',
    `login_type`  varchar(64)          DEFAULT NULL COMMENT '登录类型',
    `status`      varchar(32) NOT NULL COMMENT '登录状态',
    `fail_reason` varchar(512)         DEFAULT NULL COMMENT '失败原因',
    `client_ip`   varchar(64)          DEFAULT NULL COMMENT '客户端IP',
    `location`    varchar(256)         DEFAULT NULL COMMENT '归属地',
    `user_agent`  varchar(512)         DEFAULT NULL COMMENT 'User-Agent',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint     NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_account` (`account`),
    KEY `idx_status_create_time` (`status`, `create_time` DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户登录日志表';

-- ============================================
-- 模块：内容系统 (帖子、评论、互动)
-- ============================================

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
    `review_status`  tinyint       NOT NULL DEFAULT 0 COMMENT '审核状态(0-待审核 1-通过 2-拒绝)',
    `review_message` varchar(512)  NOT NULL DEFAULT '' COMMENT '审核信息',
    `create_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`      tinyint       NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_review_status` (`review_status`),
    FULLTEXT KEY `ft_title_content` (`title`, `content`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='帖子表';

DROP TABLE IF EXISTS `post_comment`;
CREATE TABLE `post_comment`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `content`     text     NOT NULL COMMENT '评论内容',
    `post_id`     bigint   NOT NULL COMMENT '帖子ID',
    `user_id`     bigint   NOT NULL COMMENT '评论用户ID',
    `parent_id`   bigint   NOT NULL DEFAULT 0 COMMENT '父评论ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint  NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='帖子评论表';

DROP TABLE IF EXISTS `post_thumb`;
CREATE TABLE `post_thumb`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
    `post_id`     bigint   NOT NULL COMMENT '帖子ID',
    `user_id`     bigint   NOT NULL COMMENT '用户ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_user` (`post_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='帖子点赞表';

DROP TABLE IF EXISTS `post_favour`;
CREATE TABLE `post_favour`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `post_id`     bigint   NOT NULL COMMENT '帖子ID',
    `user_id`     bigint   NOT NULL COMMENT '用户ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_user` (`post_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='帖子收藏表';

DROP TABLE IF EXISTS `SPRING_AI_CHAT_MEMORY`;
CREATE TABLE `SPRING_AI_CHAT_MEMORY`
(
    `conversation_id` varchar(128)                                 NOT NULL COMMENT '会话ID',
    `content`         text                                         NOT NULL COMMENT '消息文本内容',
    `type`            enum ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL') NOT NULL COMMENT '消息角色类型',
    `timestamp`       timestamp                                    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息创建时间',
    KEY `idx_conversation_timestamp` (`conversation_id`, `timestamp`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Spring AI 标准对话记忆表';

DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
    `name`           varchar(255) NOT NULL COMMENT '知识库名称',
    `description`    text                  DEFAULT NULL COMMENT '知识库描述',
    `user_id`        bigint       NOT NULL COMMENT '创建用户ID',
    `document_count` int                   DEFAULT 0 COMMENT '文档数量',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`      tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`) COMMENT '知识库名称唯一索引',
    KEY `idx_user_id` (`user_id`) COMMENT '创建用户ID索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='知识库表';

DROP TABLE IF EXISTS `document`;
CREATE TABLE `document`
(
    `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `knowledge_base_id`  bigint       NOT NULL COMMENT '知识库ID',
    `name`               varchar(255) NOT NULL COMMENT '文档名称',
    `file_path`          varchar(512) NOT NULL COMMENT '文件路径',
    `file_size`          bigint                DEFAULT NULL COMMENT '文件大小（字节）',
    `file_extension`     varchar(20)           DEFAULT NULL COMMENT '文件扩展名',
    `status`             varchar(20)  NOT NULL COMMENT '处理状态：PENDING/PROCESSING/COMPLETED/FAILED/TIMEOUT',
    `error_message`      text                  DEFAULT NULL COMMENT '错误信息',
    `chunk_count`        int                   DEFAULT 0 COMMENT '分片数量',
    `source_type`        varchar(64)           DEFAULT NULL COMMENT '来源类型',
    `biz_tag`            varchar(256)          DEFAULT NULL COMMENT '业务标签',
    `version`            varchar(64)           DEFAULT NULL COMMENT '版本',
    `extra_meta`         json                  DEFAULT NULL COMMENT '扩展元数据',
    `user_id`            bigint       NOT NULL COMMENT '上传用户ID',
    `upload_time`        datetime     NOT NULL COMMENT '上传时间',
    `process_start_time` datetime              DEFAULT NULL COMMENT '开始处理时间',
    `process_end_time`   datetime              DEFAULT NULL COMMENT '处理完成时间',
    `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`          tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_kb_status_delete` (`knowledge_base_id`, `status`, `is_delete`) COMMENT '知识库状态删除组合索引',
    KEY `idx_user_delete` (`user_id`, `is_delete`) COMMENT '用户文档删除索引',
    KEY `idx_status` (`status`) COMMENT '处理状态索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文档表';

DROP TABLE IF EXISTS `rag_history`;
CREATE TABLE `rag_history`
(
    `id`                bigint   NOT NULL AUTO_INCREMENT COMMENT '历史记录ID',
    `knowledge_base_id` bigint   NOT NULL COMMENT '知识库ID',
    `user_id`           bigint   NOT NULL COMMENT '用户ID',
    `question`          text     NOT NULL COMMENT '问题',
    `answer`            text     NOT NULL COMMENT '答案',
    `sources`           json              DEFAULT NULL COMMENT '引用来源（文档ID和片段ID）',
    `response_time`     bigint            DEFAULT NULL COMMENT '响应时间（毫秒）',
    `rewrite_query`     text              DEFAULT NULL COMMENT '改写后的查询',
    `retrieval_meta`    json              DEFAULT NULL COMMENT '检索元数据',
    `retrieval_strategy` varchar(64)      DEFAULT NULL COMMENT '检索策略',
    `create_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY `idx_kb_id` (`knowledge_base_id`) COMMENT '知识库ID索引',
    KEY `idx_user_id` (`user_id`) COMMENT '用户ID索引',
    KEY `idx_create_time` (`create_time`) COMMENT '创建时间索引',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='RAG问答历史表';

DROP TABLE IF EXISTS `document_chunk`;
CREATE TABLE `document_chunk`
(
    `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '分片ID',
    `document_id`       bigint       NOT NULL COMMENT '文档ID',
    `knowledge_base_id` bigint       NOT NULL COMMENT '知识库ID',
    `document_name`     varchar(255)          DEFAULT NULL COMMENT '文档名称（冗余，加速ES同步）',
    `chunk_index`       int          NOT NULL COMMENT '分片索引（从0开始）',
    `chunk_strategy`    varchar(64)           DEFAULT 'smart' COMMENT '分片策略',
    `content`           text         NOT NULL COMMENT '分片内容',
    `section_title`     varchar(500)          DEFAULT NULL COMMENT '章节标题',
    `section_path`      varchar(1000)         DEFAULT NULL COMMENT '章节路径',
    `word_count`        int                   DEFAULT 0 COMMENT '字符数',
    `token_count`       int                   DEFAULT NULL COMMENT 'Token数量',
    `vector_id`         varchar(256)          DEFAULT NULL COMMENT '向量存储ID',
    `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`         tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_document_id` (`document_id`) COMMENT '文档ID索引',
    KEY `idx_kb_id_is_delete` (`knowledge_base_id`, `is_delete`) COMMENT '知识库ID+逻辑删除索引',
    KEY `idx_doc_chunk` (`document_id`, `chunk_index`) COMMENT '文档+分片索引'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文档分片表';

-- ============================================
-- 模块：系统支撑 (通知、文件、邮件、日志)
-- ============================================

DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `title`        varchar(256) NOT NULL COMMENT '通知标题',
    `content`      text         NOT NULL COMMENT '通知内容',
    `type`         varchar(64)  NOT NULL COMMENT '通知类型',
    `biz_id`       varchar(128) NOT NULL DEFAULT '' COMMENT '业务幂等ID',
    `user_id`      bigint       NOT NULL COMMENT '接收用户ID',
    `related_id`   bigint                DEFAULT NULL COMMENT '关联对象ID',
    `related_type` varchar(64)  NOT NULL DEFAULT '' COMMENT '关联对象类型',
    `is_read`      tinyint      NOT NULL DEFAULT 0 COMMENT '是否已读',
    `status`       tinyint      NOT NULL DEFAULT 0 COMMENT '状态（0-正常，1-停用）',
    `content_url`  varchar(512) NOT NULL DEFAULT '' COMMENT '跳转链接',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`    tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_user` (`biz_id`, `user_id`),
    KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='通知表';

DROP TABLE IF EXISTS `file_upload_record`;
CREATE TABLE `file_upload_record`
(
    `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`       bigint        NOT NULL COMMENT '上传用户ID',
    `biz_type`      varchar(64)   NOT NULL COMMENT '业务类型',
    `file_name`     varchar(512)  NOT NULL COMMENT '原始文件名',
    `file_size`     bigint        NOT NULL COMMENT '文件大小',
    `file_suffix`   varchar(32)            DEFAULT NULL COMMENT '文件后缀',
    `content_type`  varchar(128)           DEFAULT NULL COMMENT '内容类型',
    `storage_type`  varchar(32)   NOT NULL COMMENT '存储类型',
    `bucket`        varchar(128)           DEFAULT NULL COMMENT '存储桶',
    `object_key`    varchar(512)  NOT NULL COMMENT '对象键/路径',
    `url`           varchar(1024) NOT NULL COMMENT '访问URL',
    `md5`           varchar(64)            DEFAULT NULL COMMENT '文件MD5',
    `client_ip`     varchar(64)            DEFAULT NULL COMMENT '客户端IP',
    `status`        varchar(32)   NOT NULL DEFAULT 'SUCCESS' COMMENT '上传状态',
    `error_message` varchar(1024)          DEFAULT NULL COMMENT '错误信息',
    `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     tinyint       NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_md5` (`md5`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文件上传记录表';

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
    `status`          varchar(32)  NOT NULL DEFAULT 'PENDING' COMMENT '发送状态',
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
    UNIQUE KEY `uk_msg_id` (`msg_id`),
    KEY `idx_status_retry` (`status`, `next_retry_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='邮件记录表';

DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log`
(
    `id`              bigint   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `operator_id`     bigint            DEFAULT NULL COMMENT '操作人ID',
    `operator_name`   varchar(128)      DEFAULT NULL COMMENT '操作人名称',
    `module`          varchar(64)       DEFAULT NULL COMMENT '模块',
    `action`          varchar(128)      DEFAULT NULL COMMENT '操作类型',
    `method`          varchar(16)       DEFAULT NULL COMMENT 'HTTP方法',
    `path`            varchar(512)      DEFAULT NULL COMMENT '请求路径',
    `request_params`  text COMMENT '请求参数',
    `response_status` int               DEFAULT NULL COMMENT '响应状态码',
    `success`         tinyint  NOT NULL DEFAULT 1 COMMENT '是否成功',
    `error_message`   varchar(1024)     DEFAULT NULL COMMENT '错误信息',
    `client_ip`       varchar(64)       DEFAULT NULL COMMENT '客户端IP',
    `location`        varchar(256)      DEFAULT NULL COMMENT '归属地',
    `create_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`       tinyint  NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_operator_id` (`operator_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='操作日志表';

DROP TABLE IF EXISTS `api_access_log`;
CREATE TABLE `api_access_log`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `trace_id`      varchar(64)           DEFAULT NULL COMMENT '链路追踪ID',
    `user_id`       bigint                DEFAULT NULL COMMENT '用户ID',
    `method`        varchar(16)  NOT NULL COMMENT 'HTTP方法',
    `path`          varchar(512) NOT NULL COMMENT '请求路径',
    `query`         varchar(1024)         DEFAULT NULL COMMENT '查询参数',
    `status`        int                   DEFAULT NULL COMMENT '响应状态码',
    `latency_ms`    int                   DEFAULT NULL COMMENT '耗时毫秒',
    `client_ip`     varchar(64)           DEFAULT NULL COMMENT '客户端IP',
    `user_agent`    varchar(512)          DEFAULT NULL COMMENT 'User-Agent',
    `referer`       varchar(512)          DEFAULT NULL COMMENT 'Referer',
    `request_size`  bigint                DEFAULT NULL COMMENT '请求大小',
    `response_size` bigint                DEFAULT NULL COMMENT '响应大小',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_trace_id` (`trace_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='接口访问日志表';
