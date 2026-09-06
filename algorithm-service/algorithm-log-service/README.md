# algorithm-log-service | MySQL 业务审计日志服务

`algorithm-log-service` 负责 `algorithm-cloud` 的业务审计日志采集与存储，包括操作日志、登录日志、API 访问日志、邮件记录和文件上传记录。它使用 MySQL 保存结构化业务记录，与可选的 Logstash/Elasticsearch 运行时技术日志链路相互补充。

## 核心能力

- 接收网关和各微服务通过 Feign 或 WebFlux 上报的审计事件。
- 使用 MyBatis-Plus 将日志写入 MySQL 并提供分页查询。
- 支持管理员删除和按保留天数定时清理历史日志。
- 记录操作、登录、API 访问、邮件和文件上传等业务事实。

## 主要 API

以下为服务内部路径；通过网关访问时通常增加 `/api` 前缀。

| 类型 | 上报 | 分页查询 |
| --- | --- | --- |
| 操作日志 | `POST /log/operation/add` | `POST /log/operation/list/page` |
| 登录日志 | `POST /log/login/add` | `POST /log/login/list/page` |
| API 访问 | `POST /log/access/add` | `POST /log/access/list/page` |
| 邮件记录 | `POST /log/email/add` | `POST /log/email/list/page` |
| 文件上传 | `POST /log/file/upload/add` | `POST /log/file/upload/list/page` |

## 数据清理

`LogCleanupJob` 按配置的 Cron 和 retention days 清理过期记录；生产环境启用前请先确认保留期限、备份策略和管理员权限。

## 运行

- 默认服务端口：`8086`
- 依赖：Nacos、MySQL；使用运行时日志汇聚时还需要 Logstash 和 Elasticsearch

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [日志 API 契约](../../algorithm-api/algorithm-api-log/README.md)
- [AOP 日志组件](../../algorithm-common/algorithm-common-log/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
