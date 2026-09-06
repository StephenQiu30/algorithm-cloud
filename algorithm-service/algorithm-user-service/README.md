# algorithm-user-service | 用户认证与 RBAC 权限服务

`algorithm-user-service` 负责 `algorithm-cloud` 的用户账号、邮箱/GitHub 登录、Token 会话、多端登录控制和 RBAC 权限校验。

## 核心能力

- 邮箱验证码登录和 GitHub OAuth 登录。
- 基于 Sa-Token 的 Token 会话、强制下线和登录限制。
- 角色、权限和 `@AuthCheck` 声明式鉴权。
- 邮箱验证码、Redis Session 和登录频率限制。
- 用户信息脱敏，以及管理员用户分页管理。

## 主要 API

以下为服务内部路径；通过网关访问时通常增加 `/api` 前缀。

| 能力 | 方法 | 路径 |
| --- | --- | --- |
| 邮箱登录 | POST | `/user/login/email` |
| GitHub 登录 | POST/GET | `/user/login/github` |
| 当前用户 | GET | `/user/get/login` |
| 退出登录 | POST | `/user/logout` |
| 用户分页 | POST | `/user/list/page/vo` |

## 技术栈

- Spring Boot、MyBatis-Plus、MySQL
- Sa-Token、Redis、Redisson
- RabbitMQ、Nacos、OpenFeign

## 运行

- 默认服务端口：`8081`
- 默认命名空间：`algorithm-cloud`
- 依赖：Nacos、MySQL、Redis、RabbitMQ，以及邮件/GitHub OAuth 配置（按登录方式启用）

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [用户 API 契约](../../algorithm-api/algorithm-api-user/README.md)
- [Nacos 配置说明](../../nacos-config/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
