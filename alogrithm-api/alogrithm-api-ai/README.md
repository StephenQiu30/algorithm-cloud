# alogrithm-api-ai - 智能化服务 API 交互层

本模块是 `alogrithm-cloud` 智能化能力的契约层，定义了各微服务调用 AI 模型能力的 Feign 接口与数据标准。

## 🌟 核心组件

- **AiFeignClient**:
    - 提供远程 AI 会话触发接口，支持同步与内联流式响应模拟。
    - 核心 DTO 包含 `AiChatRequest`, `AiChatResponse` 等。

## 🛠️ 典型应用

- **内容摘要**: 帖子服务在保存长文时，通过 RPC 调用 AI 进行内容自动提炼。
- **智能纠错**: 在用户提交评论前，通过 AI 接口进行敏感词识别与语义审核。
