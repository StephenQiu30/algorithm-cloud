# algorithm-ai-service - 智能服务

智能服务是 `algorithm-cloud` 的 AI 枢纽，通过集成前沿的大模型 (LLM) 技术，为全系统提供生成式内容推荐、智能聊天及自动化内容审核能力。

## 🌟 核心功能

- **多模型灵活适配**：
    - 基于 **LangChain4j** 构建的模型抽象层。
    - 支持平滑集成 OpenAI, 阿里通义千问 (DashScope), Ollama 等主流模型。
- **流式实时响应**：
    - 核心接口采用 **SSE (Server-Sent Events)** 协议，实现 ChatGPT 式逐字交互。
- **智能内容总结**：
    - 提供专门的总结引擎，支持对长文本、帖子内容进行结构化摘要。
- **RAG 增强 (AiSearchTool)**：
    - 集成平台搜索能力，使 AI 能够实时获取并分析系统内的帖子与数据。
- **会话与 Token 追踪**：
    - 自动追踪对话消耗，记录 Prompt/Completion Tokens。
    - 基于 **RabbitMQ** 实现对话历史与用量的异步持久化。

## 🛠️ 技术栈

- **AI SDK**: LangChain4j 0.36.2
- **核心框架**: Spring Boot 3.5.9
- **流式传输**: SSE (Server-Sent Events)
- **数据库**: MySQL (存储对话历史)

## 📡 核心 API 概览

| 模块     | 路径                    | 方法   | 描述              |
|:-------|:----------------------|:-----|:----------------|
| **对话** | `/ai/chat/stream`     | GET  | 实时流式对话 (SSE)    |
| **总结** | `/ai/summarize`       | POST | 内容总结接口          |
| **队列** | `ai_chat_record_queue`| MQ   | 异步记录持久化消费者     |

## 🚀 启动与运行

- **服务端口**: `8089`
- **依赖服务**: Nacos, 以及已配置的大模型 API Key。

---

**维护者**: StephenQiu30  
**版本**: 1.0.0
