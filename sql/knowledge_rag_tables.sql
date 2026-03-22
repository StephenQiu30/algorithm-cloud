USE algorithm;

DROP TABLE IF EXISTS `embedding_vector`;
DROP TABLE IF EXISTS `document_chunk`;
DROP TABLE IF EXISTS `knowledge_document`;
DROP TABLE IF EXISTS `knowledge_base`;

CREATE TABLE `knowledge_base`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
    `user_id`      bigint       NOT NULL COMMENT '所有者用户ID',
    `name`         varchar(256) NOT NULL COMMENT '名称',
    `description`  varchar(1024)         DEFAULT NULL COMMENT '描述',
    `status`       tinyint      NOT NULL DEFAULT 0 COMMENT '0正常 1停用',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`    tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='知识库';

CREATE TABLE `knowledge_document`
(
    `id`                 bigint        NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `knowledge_base_id`  bigint        NOT NULL COMMENT '知识库ID',
    `user_id`            bigint        NOT NULL COMMENT '上传用户ID',
    `original_name`      varchar(512)  NOT NULL COMMENT '原始文件名',
    `storage_path`       varchar(1024) NOT NULL COMMENT '本地或对象存储路径',
    `mime_type`          varchar(128)           DEFAULT NULL COMMENT 'MIME',
    `size_bytes`         bigint        NOT NULL DEFAULT 0 COMMENT '大小',
    `parse_status`       tinyint       NOT NULL DEFAULT 0 COMMENT '0待处理 1处理中 2完成 3失败',
    `error_msg`          varchar(2048)          DEFAULT NULL COMMENT '错误信息',
    `create_time`        datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`          tinyint       NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`knowledge_base_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parse_status` (`parse_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='知识库文档';

CREATE TABLE `document_chunk`
(
    `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '分片ID',
    `document_id`       bigint       NOT NULL COMMENT '文档ID',
    `knowledge_base_id` bigint       NOT NULL COMMENT '知识库ID',
    `chunk_index`       int          NOT NULL DEFAULT 0 COMMENT '序号',
    `content`           mediumtext   NOT NULL COMMENT '文本',
    `token_estimate`    int                   DEFAULT NULL COMMENT '估算token',
    `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_kb_id` (`knowledge_base_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文档分片';

CREATE TABLE `embedding_vector`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `chunk_id`         bigint       NOT NULL COMMENT '分片ID',
    `embedding_model`  varchar(128) NOT NULL COMMENT '模型名',
    `dimension`        int          NOT NULL COMMENT '维度',
    `es_doc_id`        varchar(128) NOT NULL COMMENT 'ES文档ID',
    `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chunk_id` (`chunk_id`),
    KEY `idx_es_doc` (`es_doc_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='向量元数据';
