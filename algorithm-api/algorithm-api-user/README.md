# algorithm-api-user | 用户与认证 API 契约

`algorithm-api-user` 是 `algorithm-cloud` 的用户服务接口模块，定义用户资料、登录态、角色权限和服务间用户信息查询的 Feign 客户端与 DTO/VO。

## 提供能力

- `UserFeignClient`：获取用户信息、权限状态和当前登录态。
- `UserVO`：脱敏后的用户视图对象。
- `LoginUserVO`：登录会话和 Token 相关模型。
- `UserFeignClientFallback`：远程调用失败时的降级处理。

## Maven 接入

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-api-user</artifactId>
</dependency>
```

## 启用 Feign 客户端

```java
@EnableFeignClients(basePackages = "com.algorithm.cloud.api.user.client")
```

随后即可通过依赖注入使用 `UserFeignClient`。服务名、认证和 Nacos 配置请参考后端总览及用户服务文档。

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [用户服务](../../algorithm-service/algorithm-user-service/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
