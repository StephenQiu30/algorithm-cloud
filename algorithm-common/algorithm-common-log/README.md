# algorithm-common-log | AOP 操作日志与审计组件

`algorithm-common-log` 是 `algorithm-cloud` 的分布式操作日志组件，基于 Spring AOP 和 `@OperationLog` 注解自动采集请求路径、参数、执行时间、用户和异常信息，并上报到日志服务。

## 核心能力

- **注解式记录**：在 Controller 方法上添加一个 `@OperationLog` 即可接入。
- **统一上报**：通过 `OperationLogRecorder` 和 `LogFeignClient` 发送到 `algorithm-log-service`。
- **审计字段完整**：支持用户 ID、用户名、客户端 IP、归属地、模块和操作动作。
- **异步容错**：默认异步上报并捕获异常，降低日志链路对主业务的影响。
- **可扩展落库**：实现 `OperationLogRecorder` 可覆盖默认上报逻辑。

## Maven 接入

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-common-log</artifactId>
</dependency>
```

## 使用示例

```java
@PostMapping("/review")
@OperationLog(module = "内容管理", action = "审核帖子")
public BaseResponse<Boolean> review(...) {
    // 业务逻辑
}
```

## 工作流程

```text
@OperationLog
  → OperationLogAspect
  → OperationLogContext
  → OperationLogRecorder
  → LogFeignClient
  → algorithm-log-service
```

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [日志 API 契约](../../algorithm-api/algorithm-api-log/README.md)
- [日志服务](../../algorithm-service/algorithm-log-service/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
