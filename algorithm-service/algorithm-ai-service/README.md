# algorithm-ai-service - 智能服务与 RAG 知识库

`algorithm-ai-service` 是 `algorithm-cloud` 的 AI 核心枢纽，集成了 **Spring AI** 与 **DashScope (通义千问)**。它不仅提供基础的生成式对话，更通过 **Agentic RAG** 架构实现了基于私有排序算法知识库的精准问答系统。

## 🌟 核心功能

- **Agentic RAG 问答**：通过 Tool Calling 机制，LLM 可自主决策是否检索知识库，实现“有据可依”的智能交互。
- **混合检索 (Hybrid Search)**：结合 **kNN 向量搜索**（语义相关性）与 **BM25 关键词检索**（精确匹配），确保算法术语检索的准确性。
- **RRF 排序融合**：采用 Reciprocal Rank Fusion 算法对多路检索结果进行重排序，提取最优质的上下文片段。
- **异步 ETL 流水线**：基于 RabbitMQ 和 Tika 实现文档上传、深度解析、语义分片、向量化及过程审计的全自动化流。
- **流式响应 (SSE)**：核心对话接口支持 Server-Sent Events，提供极致流畅的打字机交互体验。
- **过程审计**：通过 `embedding_vector` 表记录向量化元数据，确保数据加工链路可追溯。

## 🏗️ 核心架构解析

### 1. 知识库构建 (ETL 流程)

系统采用异步解耦架构，通过消息队列触发文档加工流水线：

1.  **文本提取**：利用 Apache Tika 自动识别并提取 PDF、Markdown 等多种格式的源文本。
2.  **内容清洗**：去除无效空白符、特殊控制字符，优化 LLM 阅读体验。
3.  **语义分片 (Chunking)**：采用 **Overlap 滚动窗口算法** 进行切分，确保前后分片语义连贯，不丢失上下文边界。
4.  **向量存储**：同步写入 MySQL (文本索引) 与 Elasticsearch (向量检索)，并记录模型版本、维度等审计信息。

### 2. RAG 检索原理 (Retrieval)

为了攻克排序算法领域“术语多、逻辑深”的痛点，我们实现了**双路召回策略**：

-   **Path A (kNN)**：提取问题 Embedding，在 ES 中进行 HNSW 高维向量搜索，捕捉语义层面的相似度。
-   **Path B (BM25)**：执行传统全文检索，确保如“QuickSort”、“小顶堆”等核心术语被精确命中。
-   **聚合与重排 (RRF)**：
    `Score = Σ (1 / (k + rank_i))`
    通过相干性分数融合，消除两路检索分值数量级不一致的问题，输出最优 Top-K 结果。

## 📊 数据库设计 (MySQL)

| 表名 | 说明 |
| :--- | :--- |
| `knowledge_base` | **知识库主体**：存储名称、描述、所有者 ID 及其多租户隔离状态。 |
| `knowledge_document` | **文档元数据**：存储原文件名、解析状态（PENDING/DONE/FAILED）及错误回溯信息。 |
| `document_chunk` | **文本片段**：存储分片后的纯文本内容、块索引及 Token 估算值。 |
| `embedding_vector` | **向量审计表**：记录向量对应 ID、使用的模型名及维度，支撑离线 ETL 链路监控。 |
| `SPRING_AI_CHAT_MEMORY` | **会话持久化**：Spring AI 标准 JDBC 存储，支持跨重启的长对话记忆。 |

## 🛠️ 技术栈清单

-   **AI Core**: [Spring AI 1.1.2](https://spring.io/projects/spring-ai)
-   **LLM Provider**: 阿里云 DashScope (qwen-plus & text-embedding-v2)
-   **Vector Engine**: Elasticsearch 8.x
-   **Message Broker**: RabbitMQ
-   **Persistent Layer**: MySQL 8.4 + MyBatis-Plus
-   **Parser**: Apache Tika

## 📡 关键 API 概览

-   `POST /knowledge/chat`: **RAG 核心接口**。接收问题及预览知识库，返回 AI 答案及引用的文本来源（Citation）。
-   `POST /knowledge/document/upload`: **入库处理接口**。上传文件后立即返回成功，解析过程异步完成。
-   `POST /chat/streamChat`: **流式对话接口**。用于常规的、非知识库绑定的 AI 对话。

## 🚀 快速启动

1.  **环境配置**: 确保 Nacos 中已配置 `aliyun.dashscope.api-key`。
2.  **基础依赖**: 确保 Elasticsearch 与 RabbitMQ 服务已就绪。
3.  **索引结构**: 首次运行将由 Spring AI 自动在 Elasticsearch 中创建 mappings。

---

**维护者**: StephenQiu30  
**版本**: 1.1.0 (RAG v2 Optimized)
