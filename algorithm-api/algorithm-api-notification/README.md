# algorithm-api-notification | 通知服务 API 契约

`algorithm-api-notification` 是 `algorithm-cloud` 的通知服务接口模块，定义系统公告、互动提醒、未读计数和消息状态管理的跨服务 RPC 协议。

## 提供能力

- `NotificationFeignClient`：创建、查询和更新通知。
- 通知 DTO/VO：统一表达通知内容、来源服务、目标用户和关联业务。
- 支持点赞、评论、系统公告和其他业务事件触发的通知。
- 可与 RabbitMQ、WebSocket 通知链路组合使用。

## Maven 接入

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-api-notification</artifactId>
</dependency>
```

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [通知服务](../../algorithm-service/algorithm-notification-service/README.md)
- [WebSocket 公共组件](../../algorithm-common/algorithm-common-websocket/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
