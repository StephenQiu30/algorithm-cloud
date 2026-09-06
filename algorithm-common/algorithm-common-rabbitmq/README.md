# algorithm-common-rabbitmq | RabbitMQ 消息与可靠投递组件

`algorithm-common-rabbitmq` 封装 RabbitMQ 的消息发送、业务分发、幂等去重、重试和死信队列能力，为 `algorithm-cloud` 的异步任务提供统一基础设施。

## 核心能力

- `RabbitMqSender`：普通发送和与事务联动的消息投递。
- `MqConsumerDispatcher`：根据业务类型将消息分发给对应处理器。
- `@RabbitMqDedupeLock`：为消费者提供声明式去重锁。
- 退避重试、死信队列（DLX）和 Jackson JSON 类型反序列化。

## Maven 接入

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-common-rabbitmq</artifactId>
</dependency>
```

## 发送消息

```java
@Resource
private RabbitMqSender mqSender;

public void publish(Object data) {
    mqSender.send(MqBizTypeEnum.USER_REGISTER, data);
    mqSender.sendTransactional(MqBizTypeEnum.POST_REVIEW, data);
}
```

## 消费消息

实现 `RabbitMqHandler<T>` 并注册为 Spring Bean，再由 `MqConsumerDispatcher` 统一分发：

```java
@Component
@RabbitMqDedupeLock(prefix = "mq:user:register")
public class UserRegisterHandler implements RabbitMqHandler<UserDTO> {
    // 实现 getBizType、onMessage 和 getDataType
}
```

## 适用场景

适合文档入库、AI 摘要、Elasticsearch 索引同步、通知和邮件等不应阻塞 HTTP 请求的异步流程。

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [Nacos 配置](../../nacos-config/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
