# alogrithm-api-post - 帖子服务 API 交互层

本模块集中管理帖子与评论服务的 Feign 客户端及其核心数据模型，为跨服务的内容引用提供了类型安全的 RPC 支撑。

## 🌟 核心组件

- **PostFeignClient**:
    - 支持根据 ID 批量获取帖子 VO 详情及基本状态校验。
- **PostCommentFeignClient**:
    - 提供对评论树数据的跨服务访问。
- **Shared Models**:
    - 核心包含 `PostVO`, `PostCommentVO` 等，确保服务间数据交换格式的一致性。

## 🛠️ 使用场景

- **搜索服务**: 在构建搜索索引时，通过 Feign 请求最新的帖子元数据。
- **通知服务**: 当产生互动（如点赞）时，通过 RPC 校验帖子是否存在并获取作者信息。
