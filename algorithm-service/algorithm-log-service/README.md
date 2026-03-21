# algorithm-log-service - 日志服务

负责**业务审计类日志**的采集与存储（操作日志、登录日志、API 访问日志、邮件/文件记录），落库 MySQL，供管理端分页查询。
与 **ELK**（logback → Logstash → ES，运行时技术日志）互补：本服务不依赖 ES，多环境下均可运行；ELK 仅在 `spring.profiles.active=prod` 且部署 Logstash 时由 logback 输出。

## 🌟 核心功能

- **分布式日志采集**：
    - 各微服务/网关通过 Feign 或 WebFlux 上报日志到本服务。
    - 本服务通过 Controller 接收请求，并由对应 Service 完成落库/更新。
- **多维检索支持**：
    - 提供分页查询接口（MyBatis-Plus：`page + LambdaQueryWrapper`）。
- **审计与追踪**：
    - 提供删除接口（管理员权限）与定时清理（根据 `log.cleanup.*` 配置）。

## 🛠️ 技术栈

- **核心框架**: Spring Boot 3.5.9, MyBatis-Plus
- **数据库**: MySQL

## 📡 核心 API 概览

| 模块 | 路径 | 方法 | 描述 |
|:---|:---|:---|:---|
| 上报-操作日志 | `/api/log/operation/add` | POST | 内部调用：写入 `operation_log` |
| 上报-登录日志 | `/api/log/login/add` | POST | 内部调用：写入 `user_login_log` |
| 上报-访问日志 | `/api/log/access/add` | POST | 内部调用：写入 `api_access_log` |
| 上报-邮件记录 | `/api/log/email/add` | POST | 内部调用：写入/更新 `email_record` |
| 上报-邮件记录(返回ID) | `/api/log/email/add/id` | POST | 内部调用：创建邮件记录并返回记录 ID |
| 更新-邮件状态 | `/api/log/email/update/status` | POST | 内部调用：更新邮件记录状态 |
| 上报-文件上传 | `/api/log/file/upload/add` | POST | 内部调用：写入 `file_upload_record` |
| 查询-操作日志 | `/api/log/operation/list/page` | POST | 管理员：分页查询 |
| 查询-访问日志 | `/api/log/access/list/page` | POST | 管理员：分页查询 |
| 查询-登录日志 | `/api/log/login/list/page` | POST | 管理员：分页查询 |
| 查询-邮件记录 | `/api/log/email/list/page` | POST | 管理员：分页查询 |
| 查询-文件上传 | `/api/log/file/upload/list/page` | POST | 管理员：分页查询 |

## 🚀 启动与运行

- **服务端口**: `8086`
- **依赖服务**: Nacos, MySQL

## 落库与清理实现

- 落库：各 Controller 分别接收 `POST /log/*/add`（以及邮件状态/返回 ID）请求，并由对应 `*LogService` 使用 MyBatis-Plus 完成 `save / updateById`。
- 清理：`LogCleanupJob` 在 `cron = "0 0 3 * * ?"` 触发；当 `log.cleanup.enabled=true` 时，按各表 retentionDays 计算阈值并批量删除（以 `createTime` 小于阈值为准）。

---

**维护者**: StephenQiu30  
**版本**: 1.0.0
