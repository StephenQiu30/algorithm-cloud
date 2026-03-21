# algorithm-post-service - 帖子服务

帖子服务是 `algorithm-cloud` 的核心内容引擎，支持高并发的内容发布、复杂的社交互动及实时的数据同步体系。

## 🌟 核心功能

- **内容矩阵**：
    - 提供完整的内容发布、草箱、编辑及逻辑删除能力。
    - 支持多级标签分类与富文本内容存储。
- **深度社交互动**：
    - **双重统计**：基于 Redis + MySQL 的高性能点赞 (Thumb) 与收藏 (Favour) 体系。
    - **多级评论**：支持盖楼式的多级评论系统，自动处理嵌套关系。
- **数据同步专家**：
    - 采用 **RabbitMQ** 驱动的数据同步流程。
    - 自动将帖子数据同步至 **Elasticsearch**，保障全系统搜索的一致性。
- **智能内容摘要**：
    - 集成 AI 服务，支持对手动或自动触发生成的帖子内容进行智能总结。
    - 采用 RabbitMQ 异步更新机制，确保内容更新与总结生成的极致性能。
- **智能推荐预处理**：
    - 自动同步 AI 总结至 Elasticsearch，增强内容的语义检索能力。

## 🛠️ 技术栈

- **核心技术**: Spring Boot 3.5.9, MyBatis-Plus
- **搜索索引**: Elasticsearch (文档同步)
- **高性能缓存**: Redis (热点统计缓存)
- **可靠消息**: RabbitMQ (解耦互动逻辑)

## 📡 核心 API 概览

| 模块     | 路径                     | 方法   | 描述      |
|:-------|:-----------------------|:-----|:--------|
| **内容** | `/post/add`            | POST | 创建新帖子   |
| **总结** | `post_summary_queue`   | MQ   | 接收 AI 总结更新  |
| **自动** | `post_auto_summary_queue`| MQ | 触发 AI 自动总结请求 |
| **互动** | `/post_thumb`          | POST | 点赞/取消点赞 |
| **社交** | `/postComment/add`     | POST | 发布评论    |
| **收藏** | `/post_favour`         | POST | 收藏/取消收藏 |

## 🚀 启动与运行

- **服务端口**: `8082`
- **依赖服务**: Nacos, MySQL, Redis, RabbitMQ, Elasticsearch

---

**维护者**: StephenQiu30  
**版本**: 1.0.0
