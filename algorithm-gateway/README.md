# algorithm-gateway

`algorithm-cloud` 微服务架构的统一 API 入口，基于 **Spring Cloud Gateway (WebFlux)** 构建。

## 核心能力

| 能力      | 说明                                                           |
|---------|--------------------------------------------------------------|
| 动态路由    | 基于 Nacos 服务发现 + LoadBalancer，按路径前缀转发到下游微服务                   |
| 统一认证    | 集成 Sa-Token (Reactor)，非白名单请求强制校验 Token，用户信息注入下游请求头           |
| 请求头净化   | 最高优先级过滤器，剥离外部伪造的敏感请求头（`from-source` / `userId` / `userName`） |
| 分布式限流   | 基于 Redis 令牌桶，支持 IP / 用户ID / API 路径三种维度限流                     |
| 访问日志    | 记录请求耗时、状态码、客户端IP，生成 `X-Trace-Id` 实现链路追踪，异步上报到日志服务            |
| 全局异常处理  | 统一 JSON 错误响应，覆盖 `401` / `503` / `504` / `500` 等场景            |
| 跨域 CORS | 全局跨域配置，`DedupeResponseHeader` 去重                             |

## 项目结构

```
gateway/
├── GatewayApplication.java           # 启动类
├── config/
│   ├── RateLimitConfig.java           # 限流 KeyResolver（IP / 用户 / 路径）
│   └── WebClientConfig.java           # LoadBalanced WebClient（日志上报）
├── constant/
│   └── GatewayConstant.java           # 网关公共常量
├── filter/
│   ├── GlobalHeaderSanitizeFilter.java # 请求头净化（order=HIGHEST_PRECEDENCE）
│   ├── GlobalAuthFilter.java          # Sa-Token 认证（order=-98）
│   └── GlobalLogFilter.java           # 访问日志 + 链路追踪（order=-200）
└── handler/
    └── GlobalExceptionHandler.java    # 全局异常处理
```

## 过滤器执行链

```
请求 → HeaderSanitize → Log(start) → Auth → 路由 → 响应 → Log(end) → 客户端
```

| 过滤器                          | Order                | 职责                                       |
|------------------------------|----------------------|------------------------------------------|
| `GlobalHeaderSanitizeFilter` | `HIGHEST_PRECEDENCE` | 剥离外部伪造的敏感请求头                             |
| `GlobalLogFilter`            | `-200`               | 记录请求开始、注入 `X-Trace-Id`、响应后上报日志           |
| `GlobalAuthFilter`           | `-98`                | Sa-Token 认证，注入 `userId` / `userName` 到下游 |

## 路由映射

| 路由 ID                            | 路径                     | 目标服务                                  | 限流策略        |
|----------------------------------|------------------------|---------------------------------------|-------------|
| `algorithm-user-service`         | `/api/user/**`         | `lb://algorithm-user-service`         | IP · 10/s   |
| `algorithm-post-service`         | `/api/post/**`         | `lb://algorithm-post-service`         | User · 20/s |
| `algorithm-search-service`       | `/api/search/**`       | `lb://algorithm-search-service`       | User · 20/s |
| `algorithm-notification-service` | `/api/notification/**` | `lb://algorithm-notification-service` | User · 20/s |
| `algorithm-file-service`         | `/api/file/**`         | `lb://algorithm-file-service`         | User · 10/s |
| `algorithm-mail-service`         | `/api/mail/**`         | `lb://algorithm-mail-service`         | IP · 5/s    |
| `algorithm-ai-service`           | `/api/ai/**`           | `lb://algorithm-ai-service`           | User · 5/s  |
| `algorithm-log-service`          | `/api/log/**`          | `lb://algorithm-log-service`          | User · 10/s |
| `algorithm-websocket-service`    | `/api/websocket/**`    | `lb://algorithm-websocket-service`    | —           |

## 运行依赖

- **Nacos** — 服务发现 + 配置中心
- **Redis** — Sa-Token 会话存储 + 限流计数

## 启动配置

| 配置项        | 值                  |
|------------|--------------------|
| 端口         | `8080`             |
| Nacos 命名空间 | `algorithm-cloud`  |
| Profile    | `default` / `prod` |

---

**维护者**: StephenQiu30 · **版本**: 1.0.0
