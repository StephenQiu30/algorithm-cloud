# algorithm-gateway | Spring Cloud Gateway API 网关

`algorithm-gateway` 是 `algorithm-cloud` 的统一 API 入口，基于 Spring Cloud Gateway WebFlux 构建，负责微服务路由、认证、请求头净化、Redis 限流、CORS、访问日志和统一异常响应。

## 核心能力

| 能力 | 说明 |
| --- | --- |
| 动态路由 | 通过 Nacos 服务发现和 LoadBalancer 将请求转发到下游服务 |
| 统一认证 | 基于 Sa-Token Reactor 校验 Token，并向内部服务传递用户上下文 |
| 请求头净化 | 移除外部伪造的 `from-source`、`userId`、`userName` 等敏感请求头 |
| 分布式限流 | 使用 Redis 令牌桶，按 IP、用户和 API 路径控制访问频率 |
| 链路日志 | 记录耗时、状态码和客户端 IP，并生成 `X-Trace-Id` |
| 异常与跨域 | 统一处理 401/500/503/504 响应并配置 CORS |

## 请求过滤器顺序

```text
请求 → HeaderSanitize → Log(start) → Auth → 路由 → 响应 → Log(end) → 客户端
```

| 过滤器 | 顺序 | 主要职责 |
| --- | ---: | --- |
| `GlobalHeaderSanitizeFilter` | `HIGHEST_PRECEDENCE` | 清理外部伪造的身份请求头 |
| `GlobalLogFilter` | `-200` | 注入 `X-Trace-Id`、记录请求和响应日志 |
| `GlobalAuthFilter` | `-98` | 执行 Sa-Token 认证并注入用户上下文 |

## 路由映射

| 路由前缀 | 目标服务 | 默认限流维度 |
| --- | --- | --- |
| `/api/user/**` | `algorithm-user-service` | IP |
| `/api/post/**` | `algorithm-post-service` | 用户 |
| `/api/search/**` | `algorithm-search-service` | 用户 |
| `/api/notification/**` | `algorithm-notification-service` | 用户 |
| `/api/file/**` | `algorithm-file-service` | 用户 |
| `/api/mail/**` | `algorithm-mail-service` | IP |
| `/api/ai/**` | `algorithm-ai-service` | 用户 |
| `/api/log/**` | `algorithm-log-service` | 用户 |

## 目录结构

```text
src/main/java/com/stephen/cloud/gateway/
├── config/       # 限流和 WebClient 配置
├── constant/     # 网关常量
├── filter/       # 认证、日志和请求头过滤器
└── handler/      # 全局异常处理
```

## 运行依赖

- Java 21 与 Maven
- Nacos：服务发现和配置中心
- Redis：登录态、限流和缓存
- 下游 `algorithm-cloud` 微服务

默认端口为 `8080`，命名空间为 `algorithm-cloud`。完整启动流程见 [algorithm-cloud/README.md](../README.md)。

## Maven 模块

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-gateway</artifactId>
</dependency>
```

本模块基于 [Apache License 2.0](../LICENSE) 开源。
