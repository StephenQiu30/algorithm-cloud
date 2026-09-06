# algorithm-common-cache | Redis、Redisson 与本地缓存组件

`algorithm-common-cache` 为 `algorithm-cloud` 微服务提供统一的缓存、分布式锁和限流能力，组合 Redis/Redisson 与 Caffeine，减少业务模块重复配置。

## 核心能力

- `CacheUtils`：统一处理对象、集合等缓存数据的序列化和读写。
- `LocalCacheUtils`：提供 JVM 进程内的 Caffeine 本地缓存。
- Redisson 分布式锁：支持看门狗续期、公平锁和读写锁等场景。
- `RateLimitUtils`：按 IP、用户或接口维度执行令牌桶限流。

## Maven 接入

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-common-cache</artifactId>
</dependency>
```

## 适用场景

- 缓存热点用户、帖子和配置数据。
- 保护注册、发帖等关键业务的并发一致性。
- 对登录、搜索和 AI 接口进行访问频率控制。

接入前请在 Nacos 中配置 Redis/Redisson 连接信息，并根据业务选择本地缓存或分布式缓存。

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [RabbitMQ 公共组件](../algorithm-common-rabbitmq/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
