# alogrithm-user-service - 用户服务

用户服务是 `alogrithm-cloud` 的基石，负责全系统的账号体系、多维身份认证、精细化权限控制及第三方登录集成。

## 🌟 核心功能

- **全方位身份认证**：
    - 支持传统的邮箱/验证码登录。
    - 集成 GitHub、微信 (扫码) 等多种社交账号登录方案。
- **智能化会话管理**：
    - 基于 **Sa-Token** 实现高性能的 Token 分发与管理。
    - 支持多端在线控制、强制下线及并发登录限制。
- **RBAC 权限体系**：
    - 提供细粒度的角色、权限校验。
    - 集成 `@AuthCheck` 注解，实现声明式权限管控。
- **安全保障**：
    - 分布式频率限制 (Rate Limiter)。
    - 完善的脱敏逻辑，确保用户信息安全。

## 🛠️ 技术栈

- **框架**: Spring Boot 3.5.9, MyBatis-Plus 3.5.12
- **认证**: Sa-Token 1.44.0
- **数据库**: MySQL 8.4
- **缓存**: Redis (用于验证码存储与分布式 Session)
- **消息**: RabbitMQ (处理异步数据统计或通知)

## 📡 核心 API 概览

| 模块      | 路径                       | 方法   | 描述           |
|:--------|:-------------------------|:-----|:-------------|
| **认证**  | `/api/user/login/email`  | POST | 邮箱验证码登录      |
| **第三方** | `/api/user/login/github` | GET  | GitHub 授权跳转  |
| **用户**  | `/api/user/get/login`    | GET  | 获取当前登录信息     |
| **管理**  | `/api/user/list/page/vo` | POST | 分页获取用户 (管理员) |

## 🚀 启动与运行

- **服务端口**: `8081`
- **默认命名空间**: `alogrithm-cloud`
- **依赖服务**: Nacos, MySQL, Redis, RabbitMQ

---

**维护者**: StephenQiu30  
**版本**: 1.0.0
