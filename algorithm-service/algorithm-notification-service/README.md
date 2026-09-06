# algorithm-notification-service | 站内通知与实时消息服务

`algorithm-notification-service` 负责 `algorithm-cloud` 的站内通知、互动提醒、未读计数和消息状态管理，并可通过 RabbitMQ 消费业务事件、通过 WebSocket 向前端推送实时消息。

## 核心能力

- 系统公告、点赞、评论和业务提醒。
- 通知创建、分页查询、已读/全读和批量操作。
- RabbitMQ 事件消费与通知分发。
- 与 WebSocket 公共组件协同，实现实时触达。
- MySQL 持久化通知及用户阅读状态。

## 主要 API

以下为服务内部路径；通过网关访问时通常增加 `/api` 前缀。

| 能力 | 方法 | 路径 |
| --- | --- | --- |
| 创建通知 | POST | `/notification/add` |
| 我的通知 | POST | `/notification/my/list/page/vo` |
| 标记已读 | POST | `/notification/read` |
| 全部已读 | POST | `/notification/read/all` |
| 未读数量 | GET | `/notification/unread/count` |
| 批量处理 | POST | `/notification/batch/read`、`/notification/batch/delete` |

## 运行

- 默认服务端口：`8083`
- 依赖：Nacos、MySQL、RabbitMQ、Redis，以及实时推送所需的 WebSocket 配置

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [通知 API 契约](../../algorithm-api/algorithm-api-notification/README.md)
- [WebSocket 公共组件](../../algorithm-common/algorithm-common-websocket/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
