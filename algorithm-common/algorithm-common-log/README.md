# algorithm-common-log - 分布式日志采集组件

基于 AOP 的无侵入式操作日志记录模块，助力微服务生态实现全链路审计与业务行为追踪。

## 🌟 核心特性

- **🚀 零侵入记录**: 仅需一个 `@OperationLog` 注解即可自动采集请求路径、参数、执行时间及异常堆栈。
- **🧩 统一上报**: 默认由 `OperationLogRecorderImpl` 将 `OperationLogContext` 映射为 `OperationLogAddRequest`，并通过
  `LogFeignClient` 上报到 `algorithm-log-service`（`POST /api/log/operation/add`）。如需特殊落放逻辑，可在微服务内实现
  `OperationLogRecorder` 覆盖默认实现。
- **📊 维度丰富**: 自动采集当前登录用户（`userId/userName`）、客户端 IP 及归属地 `location`，并携带模块名称及操作动作。
- **🛡️ 生产级可靠**: 基于 `@Async` 异步处理，上报异常会被捕获，避免影响核心业务吞吐。

## 🏗️ 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-common-log</artifactId>
</dependency>
```

### 2. 使用方法

在 Controller 方法上标注：

```java
@PostMapping("/update")
@OperationLog(module = "订单中心", action = "更新订单状态")
public BaseResponse<Boolean> updateOrder(...) {
    // 业务逻辑
}
```

## 🛠️ 内部机制

- **切面拦截**: `OperationLogAspect` 统一拦截标注了注解的方法，并构建 `OperationLogContext`。
- **上下文感知**: 从请求头读取 `userId/userName`，计算 `clientIp/location`，并将上下文透传给
  `OperationLogRecorder#recordOperationLogAsync`。
- **默认落库上报**: `OperationLogRecorderImpl` 调用 `LogFeignClient#addOperationLog`，由 `algorithm-log-service`
  对应控制器接收并落库。

---

**维护者**: StephenQiu30  
**版本**: 1.0.0
