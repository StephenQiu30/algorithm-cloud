# 排序算法教学 RAG 平台后端 | Spring Cloud Alibaba 微服务

[![Maven CI](https://github.com/StephenQiu30/algorithm-cloud/actions/workflows/maven.yml/badge.svg)](https://github.com/StephenQiu30/algorithm-cloud/actions/workflows/maven.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

`algorithm-cloud` 是一个面向排序算法教学的开源 RAG（Retrieval-Augmented Generation，检索增强生成）交互式系统后端。项目基于 Java 21、Spring Boot、Spring Cloud Alibaba、Spring AI、Elasticsearch、RabbitMQ、MySQL、Redis 和 Nacos 构建，为算法可视化课堂提供用户认证、内容社区、知识库、文档解析、混合检索、流式问答和运营审计能力。

如果你正在寻找 **Spring Cloud Alibaba 微服务示例、Spring AI RAG 项目、Elasticsearch 向量检索、RabbitMQ 异步任务、Nacos 配置中心或排序算法教学平台后端**，本仓库提供了一个可以继续扩展的完整工程骨架。

## 项目关系

- [algorithm-next](https://github.com/StephenQiu30/algorithm-next)：排序算法可视化课堂和用户侧前端。
- [algorithm-admin](https://github.com/StephenQiu30/algorithm-admin)：React + Ant Design Pro 管理后台。
- 当前仓库：Java 微服务后端、RAG 知识库和部署配置。

## 核心能力

- **RAG 智能问答**：解析 Markdown、PDF、Word 等教学材料，生成文档分片和向量索引，支持 SSE 流式回答。
- **混合检索**：并行使用 Elasticsearch 向量检索和 BM25 全文检索，再通过 RRF（Reciprocal Rank Fusion）融合结果。
- **知识库管理**：支持知识库、文档、文档分片、召回分析和检索结果追踪。
- **内容社区**：用户、帖子、评论、点赞、收藏、审核、搜索和内容摘要。
- **微服务治理**：Nacos 服务发现与配置、Spring Cloud Gateway API 网关、Sa-Token 认证、Redis 限流和统一异常处理。
- **异步任务链路**：使用 RabbitMQ 解耦文档入库、AI 摘要、搜索同步和通知等耗时任务。
- **可观测与审计**：业务操作日志、登录日志、API 访问日志，以及可选的 Logstash、Prometheus 和 Grafana 基础设施。
- **对象存储适配**：文件服务支持腾讯云 COS 和开发环境中的 MinIO。

## RAG 核心链路

```text
教学文档上传
    ↓
文档解析与文本清洗
    ↓
段落合并、语义切片与重叠窗口
    ↓
MySQL 持久化 + Elasticsearch 向量索引
    ↓
向量召回 + BM25 关键词召回
    ↓
RRF 融合排序
    ↓
Spring AI 组织上下文并通过 SSE 流式回答
```

这条链路把排序算法原理、代码片段、时间复杂度和空间复杂度等教学内容纳入可追踪的知识库，便于调试召回效果和持续改进问答质量。

## 微服务模块

| 模块 | 主要职责 | 本地端口 |
| --- | --- | ---: |
| `algorithm-gateway` | API 网关、路由、认证、限流、请求头净化和链路日志 | 8080 |
| `algorithm-user-service` | 邮箱/GitHub 登录、用户资料、角色和权限 | 8081 |
| `algorithm-post-service` | 帖子、评论、点赞、收藏、审核和内容同步 | 8082 |
| `algorithm-notification-service` | 站内通知、未读数和实时消息触达 | 8083 |
| `algorithm-search-service` | Elasticsearch 全文检索、聚合搜索和索引同步 | 8084 |
| `algorithm-file-service` | 文件上传、校验、COS/MinIO 对象存储 | 8085 |
| `algorithm-log-service` | 业务审计、登录、访问、邮件和文件记录 | 8086 |
| `algorithm-mail-service` | 验证码、模板邮件和异步邮件投递 | 8087 |
| `algorithm-ai-service` | 知识库、文档处理、RAG 问答和召回分析 | 8088 |

公共能力和跨服务契约见：

- [API 契约模块](algorithm-api/)
- [公共组件模块](algorithm-common/)
- [网关模块](algorithm-gateway/README.md)
- [服务模块](algorithm-service/)
- [Nacos 配置](nacos-config/README.md)
- [SQL 初始化](sql/README.md)

## 技术栈

| 领域 | 技术 |
| --- | --- |
| 运行环境 | Java 21、Maven |
| Web 与微服务 | Spring Boot 3.x、Spring Cloud、Spring Cloud Alibaba、Spring Cloud Gateway |
| AI 与 RAG | Spring AI、DashScope、文档解析、Elasticsearch Vector Store |
| 数据与缓存 | MySQL、MyBatis-Plus、Redis、Redisson、Caffeine |
| 消息与实时通信 | RabbitMQ、Netty、WebSocket |
| 服务治理 | Nacos、Sa-Token、OpenFeign、Knife4j |
| 搜索与观测 | Elasticsearch、Logstash、Prometheus、Grafana |

## 快速开始

### 1. 准备环境

请先安装：

- JDK 21
- Maven 3.9+
- Docker 和 Docker Compose
- 可用的 DashScope/LLM 配置（使用 AI 服务时）

### 2. 启动基础设施

在当前目录复制并补全环境变量。`.env.example` 是安全模板，不包含可直接使用的生产凭证：

```bash
cp .env.example .env
# 根据 docker-compose-env.yml 补充数据库、Redis、Nacos、RabbitMQ 等变量
docker compose -f docker-compose-env.yml up -d
```

基础设施启动后，访问 Nacos：<http://localhost:8848/nacos>；RabbitMQ 管理台默认地址为 <http://localhost:15672>。

### 3. 初始化数据库和 Nacos 配置

```bash
mysql -u root -p < sql/algorithm.sql

cp nacos-config/common-secret.properties.example \
   nacos-config/common-secret.properties
# 在 common-secret.properties 中填入数据库、Redis、对象存储和 AI API 配置
chmod +x nacos-config/import-config.sh
./nacos-config/import-config.sh
```

敏感配置文件已被 Git 忽略，请勿提交真实密码、Token 或 API Key。

### 4. 构建并启动服务

```bash
mvn clean install -DskipTests
docker compose up -d --build
```

本地开发也可以直接在 IDE 中启动各个 `*Application` 入口类。完整配置说明见 [nacos-config/README.md](nacos-config/README.md)。

## 开发与验证

```bash
# 编译全部 Maven 模块
mvn clean install

# 跳过测试构建
mvn clean install -DskipTests
```

提交代码前请同步更新受影响模块的 README、配置示例和 API 文档，并确保没有把本地凭证或生成物提交到 Git。

## 文档与学习资料

- [排序算法学习路径与知识图谱](docs/18-排序算法学习路径与知识图谱.md)
- [排序算法综合对比](docs/09-排序算法综合对比.md)
- [排序算法工程实践](docs/10-排序算法的工程实践.md)
- [排序算法面试高频问题](docs/12-排序算法的面试高频问题.md)
- [全部算法文档](docs/)

## 参与贡献

欢迎通过 [Issues](https://github.com/StephenQiu30/algorithm-cloud/issues) 反馈问题，或提交 Pull Request 改进算法教学、RAG 检索、微服务能力和文档。建议在提交前说明变更背景、验证方式和可能影响的服务。

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 开源。

## 维护者

[StephenQiu30](https://github.com/StephenQiu30)
