# algorithm-api-mail | 邮件服务 API 契约

`algorithm-api-mail` 是 `algorithm-cloud` 的邮件微服务接口模块，统一封装验证码邮件、同步邮件和跨服务安全校验所需的 Feign 客户端与请求模型。

## 提供能力

- `MailFeignClient`：发送邮件和校验验证码。
- `EmailCodeRequest` 等邮件请求 DTO。
- 为注册、登录、账号安全和系统通知提供统一 RPC 协议。

## Maven 接入

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-api-mail</artifactId>
</dependency>
```

## 使用示例

```java
EmailCodeRequest request = new EmailCodeRequest();
request.setEmail("target@example.com");
mailFeignClient.sendEmailCode(request);

boolean valid = mailFeignClient.verifyEmailCode(request).getData();
```

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [邮件服务](../../algorithm-service/algorithm-mail-service/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
