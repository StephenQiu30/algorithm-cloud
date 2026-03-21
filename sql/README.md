# SQL 数据库初始化脚本

本目录包含 `alogrithm-cloud` 全系统所需的数据库定义（DDL）。建议在项目启动初期，按照本指南完成数据库环境的初始化。

## 🗄️ 数据库

当前为单库 `alogrithm_cloud`，各业务表在同一库内按表名前缀区分，例如：`user`、`post`、`post_comment`、`post_thumb`、`post_favour`、`notification`、`ai_chat_record`、`api_access_log`、`operation_log`、`user_login_log`、`email_record`、`file_upload_record` 等。

## 🛠️ 初始化步骤

### 1. 创建数据库

首先执行创建数据库的脚本：

```bash
mysql -u root -p < create_databases.sql
```

### 2. 初始化表结构 (自动化)

推荐使用以下命令一键初始化所有表：

```bash
for file in *_table.sql; do
  mysql -u root -p < "$file"
done
# 特殊命名的业务表
mysql -u root -p < ai_chat_record.sql
```

## 📝 表设计规范

- **字符集**: 统一使用 `utf8mb4`，支持 Emoji 存储。
- **公共字段**:
    - `id`: 雪花算法 ID 或自增 ID。
    - `create_time`: 创建时间，系统自动维护。
    - `update_time`: 修改时间，系统自动维护（用于 ES 增量同步）。
    - `is_delete`: 逻辑删除位（0-正常，1-删除）。
- **SQL 标准**: 包含 `DROP TABLE IF EXISTS`，支持幂等执行。

## ⚠️ 注意事项

1. **环境差异**: 生产环境建议通过 Nacos 或环境变量动态注入数据库连接信息。
2. **性能优化**: 日志类表（如 `api_access_log`）建议定期归档或清理。
3. **安全**: 避免修改 `root` 权限直接运行，建议为每个库创建独立账号。
