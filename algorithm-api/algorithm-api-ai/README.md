# algorithm-api-ai | RAG 与 AI 服务 API 契约

`algorithm-api-ai` 是 `algorithm-cloud` 的 AI 微服务接口模块，使用 Spring Cloud OpenFeign、DTO 和 VO 定义知识库、文档处理、RAG 流式问答与召回分析所需的跨服务通信契约。

## 提供能力

- AI 服务 Feign 客户端和统一响应模型。
- RAG 问答请求、历史记录和 SSE 流式事件模型。
- 知识库、文档、文本分片和召回分析 DTO/VO。
- 为帖子摘要、内容审核和其他服务调用 AI 能力提供类型安全的边界。

## Maven 接入

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-api-ai</artifactId>
</dependency>
```

## 适用场景

当业务服务需要调用 `algorithm-ai-service`，或前后端需要共享 RAG 数据结构时，应优先复用本模块的客户端和模型，避免重复定义接口对象。

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [AI 服务](../../algorithm-service/algorithm-ai-service/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
