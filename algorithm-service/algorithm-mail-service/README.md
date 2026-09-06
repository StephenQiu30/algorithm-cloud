# algorithm-mail-service | 验证码与异步邮件服务

`algorithm-mail-service` 为 `algorithm-cloud` 提供验证码、账号安全、系统告警和业务通知邮件能力，支持同步发送、RabbitMQ 异步投递和模板渲染。

## 核心能力

- 同步邮件发送，适合需要即时反馈的业务。
- RabbitMQ 异步邮件发送，适合批量或非阻塞通知。
- 注册、登录等验证码模板。
- Thymeleaf HTML 模板渲染。
- 重试、幂等和死信队列配置，提升投递链路可恢复性。

## 主要 API

以下为服务内部路径；通过网关访问时通常增加 `/api` 前缀。

| 能力 | 方法 | 路径 |
| --- | --- | --- |
| 同步发送 | POST | `/mail/send/sync` |
| 异步发送 | POST | `/mail/send/async` |
| 发送验证码 | POST | `/mail/send/verification-code` |

## 技术栈与配置

- Spring Boot Mail（`JavaMailSender`）
- RabbitMQ、Spring Retry、死信队列
- Thymeleaf 邮件模板
- SMTP 服务：QQ 邮箱、Gmail 或其他兼容服务

## 运行

- 默认服务端口：`8087`
- 依赖：Nacos、RabbitMQ 和可用的 SMTP 服务器
- 邮箱账号、密码和 SMTP 地址请放入 Nacos 敏感配置，不要提交到 Git

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [邮件 API 契约](../../algorithm-api/algorithm-api-mail/README.md)
- [Nacos 配置说明](../../nacos-config/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
