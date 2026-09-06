# algorithm-common-core | Java 微服务核心公共组件

`algorithm-common-core` 是 `algorithm-cloud` 的基础公共模块，提供统一响应、异常处理、分页模型、公共枚举和通用工具，帮助各 Spring Boot 微服务保持一致的 API 行为。

## 核心能力

- `BaseResponse` 与 `ResultUtils`：统一封装成功和失败响应。
- `BusinessException` 与 `ThrowUtils`：统一业务异常和参数校验。
- `PageRequest`：分页、排序和查询请求的基础模型。
- Spring 上下文持有、错误码、用户角色和文件业务类型等公共元数据。

## Maven 接入

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-common-core</artifactId>
</dependency>
```

## 适用场景

所有业务服务都可以依赖该模块，以复用基础响应协议和异常规范，避免跨服务返回结构不一致。

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [Web 公共组件](../algorithm-common-web/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
