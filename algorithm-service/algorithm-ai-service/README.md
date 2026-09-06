# algorithm-ai-service | Spring AI RAG 智能问答服务

`algorithm-ai-service` 是 `algorithm-cloud` 的 AI 核心服务，基于 Spring AI、DashScope 和 Elasticsearch Vector Store 实现排序算法知识库、文档解析、混合检索、流式问答和召回效果分析。

## 核心能力

- **RAG 流式问答**：通过 SSE 输出回答和结构化阶段事件。
- **知识库管理**：创建、编辑、删除和查询知识库。
- **文档处理**：上传和解析 Markdown、PDF、Word 等教学资料。
- **语义分片**：清洗文本、合并段落、切分文档并记录分片元数据。
- **混合召回**：结合 Elasticsearch 向量检索与全文关键词检索，并支持 RRF 融合。
- **效果分析**：对单个或批量问题执行召回相关性分析。
- **会话记忆**：持久化 RAG 历史和聊天上下文，便于连续学习。

## 主要 API

以下为服务内部路径；通过网关访问时通常增加 `/api` 前缀。

| 能力 | 方法 | 路径 |
| --- | --- | --- |
| RAG 流式问答 | POST | `/ai/rag/ask/stream` |
| 结构化 SSE 问答 | POST | `/ai/rag/ask/stream/events` |
| RAG 历史 | POST | `/ai/rag/history/list/page/vo` |
| 召回分析 | POST | `/ai/rag/recall/analyze` |
| 批量召回分析 | POST | `/ai/rag/recall/batch/analyze` |
| 文档上传 | POST | `/ai/doc/add` |
| 知识库管理 | POST/GET | `/ai/kb/**` |
| 分片检索 | POST | `/ai/chunk/search` |

## 技术栈与依赖

- Java 21、Spring Boot、Spring AI
- DashScope 对话模型与 Embedding 模型
- Elasticsearch 向量存储与全文检索
- MySQL、Redis、RabbitMQ、Nacos
- 文件解析：Tika、Markdown 和 PDF Reader

## 运行

- 默认服务端口：`8088`
- 依赖：Nacos、MySQL、Redis、RabbitMQ、Elasticsearch，以及已配置的模型服务
- 配置导入：参考 [Nacos 配置说明](../../nacos-config/README.md)

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [AI API 契约](../../algorithm-api/algorithm-api-ai/README.md)
- [排序算法学习资料](../../docs/18-排序算法学习路径与知识图谱.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
