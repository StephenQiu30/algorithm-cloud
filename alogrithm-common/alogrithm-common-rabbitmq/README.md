# alogrithm-common-rabbitmq - 消息队列基础设施

封装 RabbitMQ 核心操作，提供基于 **策略模式** 的标准化分发、**声明式幂等** 与 **事务一致性** 投递方案。

## 🌟 核心功能

- **统一投递门面 (`RabbitMqSender`)**:
    - 支持普通发送与 **事务发送**（与 Spring @Transactional 联动）。
    - 自动封装 `RabbitMessage` 元数据。
- **标准化分发器 (`MqConsumerDispatcher`)**:
    - 基于业务类型 (`BizType`) 自动路由到对应处理器。
- **声明式去重锁 (`@RabbitMqDedupeLock`)**:
    - 仅需在处理器上添加注解，即可实现自动去重。
- **高可靠性保障**:
    - 集成退避重试 (Exponential Backoff) 与死信队列 (DLX)。
    - Jackson2Json 反序列化，确保类型安全。

## 🛠️ 注册与使用指南

### 1. 发送消息 (Producer)

注入 `RabbitMqSender` 并指定业务类型：

```java
@Resource
private RabbitMqSender mqSender;

public void doTask() {
    // 1. 普通发送
    mqSender.send(MqBizTypeEnum.USER_REGISTER, userData);

    // 2. 事务投递（确保事务提交后才发送）
    mqSender.sendTransactional(MqBizTypeEnum.POST_REVIEW, postData);
}
```

### 2. 定义处理逻辑 (Handler)

实现 `RabbitMqHandler<T>` 接口并注册为 Spring Bean：

```java
@Component
@RabbitMqDedupeLock(prefix = "mq:user:register")
public class UserRegisterHandler implements RabbitMqHandler<UserDTO> {

    @Override
    public String getBizType() {
        return MqBizTypeEnum.USER_REGISTER.getValue();
    }

    @Override
    public void onMessage(UserDTO data, RabbitMessage rabbitMessage) {
        // 执行业务逻辑
    }

    @Override
    public Class<UserDTO> getDataType() {
        return UserDTO.class;
    }
}
```

### 3. 配置监听器 (Consumer)

在消费者服务中调用 `MqConsumerDispatcher` 进行统一分发：

```java
@RabbitListener(queues = RabbitMqConstant.CORE_QUEUE)
public void onMessage(RabbitMessage rabbitMessage, Channel channel, Message msg) {
    mqConsumerDispatcher.dispatch(rabbitMessage, channel, msg);
}
```

