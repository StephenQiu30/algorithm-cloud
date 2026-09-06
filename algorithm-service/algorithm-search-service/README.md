# algorithm-search-service | Elasticsearch 全文与聚合搜索服务

`algorithm-search-service` 基于 Elasticsearch 为 `algorithm-cloud` 提供帖子、用户和多数据源聚合搜索，并负责搜索索引的初始化与增量同步。

## 核心能力

- 帖子标题、正文、标签和用户信息的全文检索。
- 搜索结果关键词高亮。
- 一次请求聚合帖子、用户等多个数据源。
- 通过 RabbitMQ/Feign 接收内容变更并更新索引。
- 可与 AI 服务的向量检索能力组合，构建混合搜索体验。

## 主要 API

以下为服务内部路径；通过网关访问时通常增加 `/api` 前缀。

| 能力 | 方法 | 路径 |
| --- | --- | --- |
| 聚合搜索 | POST | `/search/all` |
| 帖子搜索 | POST | `/search/post/page` |
| 用户搜索 | POST | `/search/user/page` |

## 技术栈

- Elasticsearch Java Client
- Elasticsearch 8.x 本地开发环境
- Spring Boot、RabbitMQ、Nacos、Redis
- 中文全文检索和高亮配置按部署环境启用

## 运行

- 默认服务端口：`8084`
- 依赖：Nacos、Elasticsearch、RabbitMQ（索引同步）
- 索引配置和连接信息见 `nacos-config/`。

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [搜索 API 契约](../../algorithm-api/algorithm-api-search/README.md)
- [AI RAG 服务](../algorithm-ai-service/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
