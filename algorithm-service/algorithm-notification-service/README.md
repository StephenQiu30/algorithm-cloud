# algorithm-notification-service - 通知服务

通知服务提供全方位的消息触达能力，确保系统通知与用户互动能够实时、准确地传递给目标用户。

## 🌟 核心功能

- **统一消息管理**：
    - 处理系统公告、业务通知及互动提醒（点赞、评论通知）。
    - 支持标记已读、一键全读及未读数实时统计。
- **智能化分发**：
    - 集成 RabbitMQ 消费来自其他服务的交互事件。
    - 支持按需生成私信消息或全员广播。
- **实时性保障**：
    - 与 `algorithm-websocket-service` 协同，实现 Web 端的毫秒级消息推送。

## 🛠️ 技术栈

- **核心框架**: Spring Boot 3.5.9, MyBatis-Plus
- **消息总线**: RabbitMQ (事件驱动)
- **数据存储**: MySQL 8.4
- **实时推送**: Netty (协作模块)

## 📡 核心 API 概览

| 模块     | 路径                               | 方法   | 描述         |
|:-------|:---------------------------------|:-----|:-----------|
| **基础** | `/api/notification/add`          | POST | 创建通知 (管理员) |
| **交互** | `/api/notification/read`         | POST | 标记通知已读     |
| **统计** | `/api/notification/unread/count` | GET  | 获取未读消息总数   |
| **列表** | `/api/notification/list/page`    | POST | 分页查询我的通知   |

## 🚀 启动与运行

- **服务端口**: `8083`
- **依赖服务**: Nacos, MySQL, RabbitMQ

---

**维护者**: StephenQiu30  
**版本**: 1.0.0
