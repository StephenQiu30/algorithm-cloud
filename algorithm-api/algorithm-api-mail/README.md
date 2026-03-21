# algorithm-api-mail - 邮件服务 API 交互层

本模块封装了邮件服务的远程调用协议，为全系统提供了统一、安全的邮件下发与验证码管理 RPC 能力。

## 🌟 核心组件

- **MailFeignClient**:
    - 提供 `sendEmailCode` 同步发送验证码接口。
    - 提供 `verifyEmailCode` 跨服务安全校验入口。

## 🛠️ 接入流程

### 1. 发送逻辑

```java
EmailCodeRequest req = new EmailCodeRequest();
req.setEmail("target@example.com");
mailFeignClient.sendEmailCode(req);
```

### 2. 校验逻辑 (业务层使用)

```java
EmailCodeRequest verifyReq = new EmailCodeRequest();
verifyReq.setEmail(email);
verifyReq.setCode(authCode);
boolean isValid = mailFeignClient.verifyEmailCode(verifyReq).getData();
```
