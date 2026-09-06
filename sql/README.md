# MySQL 数据库初始化 | algorithm-cloud SQL

本目录提供 `algorithm-cloud` 的单库 MySQL 初始化脚本，覆盖用户、登录、帖子、评论、点赞、收藏、RAG 知识库、文档、聊天记忆、通知、文件、邮件和审计日志等业务表。

## 初始化方式

当前目录的主脚本为 [`algorithm.sql`](algorithm.sql)，脚本会创建 `algorithm` 数据库并初始化全部表结构：

```bash
mysql -u root -p < algorithm.sql
```

也可以在 MySQL 客户端中执行：

```sql
SOURCE /absolute/path/to/algorithm-cloud/sql/algorithm.sql;
```

## 数据表范围

- **用户与认证**：`user`、`user_login_log`
- **内容与互动**：`post`、`post_comment`、`post_thumb`、`post_favour`
- **AI 与 RAG**：`SPRING_AI_CHAT_MEMORY`、`knowledge_base`、`document`、`document_chunk`、`rag_history`
- **系统支撑**：`notification`、`file_upload_record`、`email_record`
- **审计与访问**：`operation_log`、`api_access_log`

## 表设计约定

- 字符集为 `utf8mb4`，支持中文和 Emoji。
- 常见公共字段包括 `id`、`create_time`、`update_time` 和 `is_delete`。
- 脚本包含 `DROP TABLE IF EXISTS`，重复执行前请确认已有数据已备份。
- 生产环境不要直接使用 root 运行应用；请创建权限最小化的专用数据库账号。
- 日志和历史记录应结合保留策略定期归档或清理。

## 连接配置

数据库连接不写入代码或 README。请在 Nacos 的敏感配置中设置 JDBC URL、用户名和密码，参考 [`common-secret.properties.example`](../nacos-config/common-secret.properties.example)。

## 相关文档

- [algorithm-cloud 后端总览](../README.md)
- [Nacos 配置说明](../nacos-config/README.md)
- [SQL 脚本](algorithm.sql)

本目录基于 [Apache License 2.0](../LICENSE) 开源。
