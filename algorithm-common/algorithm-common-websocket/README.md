# algorithm-common-websocket | Netty WebSocket 实时通信组件

`algorithm-common-websocket` 基于 Netty 为 `algorithm-cloud` 提供 WebSocket 长连接、用户会话管理、心跳检测和多端消息推送能力。

## 核心能力

- 异步非阻塞 WebSocket 握手、读写和心跳处理。
- `WebSocketSessionManager`：按用户 ID 管理连接和消息路由。
- 支持多端登录下的连接绑定和消息广播。
- 在握手阶段结合 Sa-Token 执行 Token 鉴权。
- 使用结构化 JSON 消息承载实时通知和互动事件。

## Maven 接入

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-common-websocket</artifactId>
</dependency>
```

## 适用场景

- 通知服务向 Web 端推送未读消息和系统提醒。
- 维护用户在线/离线状态。
- 为评论互动、聊天和实时课堂功能提供低延迟通道。

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [通知 API 契约](../../algorithm-api/algorithm-api-notification/README.md)
- [通知服务](../../algorithm-service/algorithm-notification-service/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
