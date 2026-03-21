# algorithm-api-user - 用户服务 API 交互层

本模块定义了用户服务对外提供的 Feign 客户端及其关联的传输对象 (DTO) 与视图对象 (VO)，是微服务间通信的标准协议层。

## 🌟 核心组件

- **UserFeignClient**:
    - 提供跨服务获取用户信息、权限状态及当前登录态的 RPC 接口。
- **Shared Models**:
    - `UserVO`: 经过数据脱敏的用户视图。
    - `LoginUserVO`: 包含 Token 的会话状态模型。
- **Fallback**:
    - 内置 `UserFeignClientFallback`，处理服务熔断与异常降级，保障调用稳定性。

## 🚀 接入说明

1. **引入依赖**: 在消费方 pom.xml 中引用本模块。
2. **开启扫描**: 启动类添加 `@EnableFeignClients(basePackages = "com.algorithm.cloud.api.user.client")`。
3. **注入使用**: 通过 `@Resource` 注入 `UserFeignClient` 即可进行同步/异步调用。
