# algorithm-post-service | 帖子、评论与内容互动服务

`algorithm-post-service` 是 `algorithm-cloud` 的内容服务，提供帖子发布、Markdown 内容管理、评论、点赞、收藏、审核，以及向 Elasticsearch 和 AI 服务同步内容的能力。

## 核心能力

- 帖子创建、编辑、审核、逻辑删除和分页查询。
- 多级评论树、点赞和收藏。
- Redis + MySQL 支撑热点互动数据。
- RabbitMQ 驱动 Elasticsearch 增量同步。
- 调用 AI 服务生成内容摘要，增强搜索和 RAG 内容利用。

## 主要 API

以下为服务内部路径；通过网关访问时通常增加 `/api` 前缀。

| 能力 | 方法 | 路径 |
| --- | --- | --- |
| 创建帖子 | POST | `/post/add` |
| 帖子分页 | POST | `/post/list/page/vo` |
| 评论发布 | POST | `/post/comment/add` |
| 点赞/取消点赞 | POST | `/post/thumb/add` |
| 收藏/取消收藏 | POST | `/post/favour/add` |
| 帖子审核 | POST | `/post/review` |

## 异步事件

帖子变更会触发搜索索引和 AI 摘要相关消息。消费者、队列名称和重试策略由 RabbitMQ 公共组件与 Nacos 配置统一管理。

## 运行

- 默认服务端口：`8082`
- 依赖：Nacos、MySQL、Redis、RabbitMQ、Elasticsearch

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [帖子 API 契约](../../algorithm-api/algorithm-api-post/README.md)
- [RabbitMQ 公共组件](../../algorithm-common/algorithm-common-rabbitmq/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
