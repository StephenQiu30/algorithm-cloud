# algorithm-common-web | Spring Boot Web 与 OpenAPI 公共组件

`algorithm-common-web` 统一 `algorithm-cloud` 各微服务的 Web 层行为，提供 Jackson 序列化、日期格式、CORS 和 Knife4j/OpenAPI 文档配置。

## 核心能力

- Long 类型序列化为 String，避免前端 JavaScript 整数精度问题。
- 统一日期时间格式 `yyyy-MM-dd HH:mm:ss`。
- 集成 Knife4j，生成可交互的 OpenAPI 调试文档。
- 提供跨服务一致的 CORS 配置和凭证透传策略。

## Maven 接入

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-common-web</artifactId>
</dependency>
```

## 适用场景

所有对外提供 HTTP API 的 Spring Boot 服务都可以复用该模块，以保持响应序列化、跨域和 API 文档体验一致。

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [核心公共组件](../algorithm-common-core/README.md)
- [API 网关](../../algorithm-gateway/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
