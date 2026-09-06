# algorithm-api-log | 日志与审计 API 契约

`algorithm-api-log` 是 `algorithm-cloud` 的统一日志 API 模块，为用户服务、网关和其他微服务提供业务审计、登录日志、访问日志、邮件记录和文件记录的跨服务上报契约。

## 提供能力

- `LogFeignClient`：统一日志上报入口。
- `BaseLogDTO` 及操作、登录、访问、邮件、文件记录模型。
- 跨服务一致的日志字段和请求结构。
- 适合与 `algorithm-common-log` 的 AOP 组件组合使用。

## Maven 接入

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-api-log</artifactId>
</dependency>
```

## 接入建议

业务服务通过 Feign 上报日志，日志服务负责落库和查询。日志上报应保持异步或非阻塞，避免审计链路影响核心业务请求。

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [公共日志组件](../../algorithm-common/algorithm-common-log/README.md)
- [日志服务](../../algorithm-service/algorithm-log-service/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
