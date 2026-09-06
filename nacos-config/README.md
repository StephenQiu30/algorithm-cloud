# Nacos 配置中心 | 微服务环境配置导入

`nacos-config` 保存 `algorithm-cloud` 的 Nacos 配置模板，为 Spring Cloud Alibaba 微服务提供服务发现、数据库、Redis、RabbitMQ、Elasticsearch、对象存储、邮件和 AI 模型配置。

## 配置文件分类

| 文件模式 | 用途 |
| --- | --- |
| `common-*.yml` | MySQL、Redis、RabbitMQ、Web、AI、对象存储等公共配置 |
| `common-*-prod.yml` | 生产环境配置覆盖 |
| `common-secret.properties.example` | 开发环境敏感配置模板 |
| `common-secret-prod.properties.example` | 生产环境敏感配置模板 |
| `import-config.sh` | 调用 Nacos OpenAPI 批量导入配置 |

## 快速导入

### 1. 启动 Nacos

```bash
docker compose -f ../docker-compose-env.yml up -d nacos
```

默认控制台地址为 <http://localhost:8848/nacos>，默认命名空间为 `algorithm-cloud`，配置分组为 `DEFAULT_GROUP`。如果你修改了 Nacos 地址、用户名或密码，请同步检查 `import-config.sh` 中的变量。

### 2. 创建本地敏感配置

```bash
cp common-secret.properties.example common-secret.properties
```

然后补充 MySQL、Redis、RabbitMQ、Elasticsearch、COS/MinIO、邮件、OAuth 和 DashScope 等凭证。`common-secret.properties` 已被 Git 忽略，真实密钥不得提交。

### 3. 导入配置

```bash
chmod +x import-config.sh
./import-config.sh
```

脚本会扫描当前目录中的 YAML/Properties 配置并批量写入 Nacos。导入后请在 Nacos 控制台确认配置数量和命名空间。

## 开发与生产环境

- 开发环境使用 `common-*.yml` 与 `common-secret.properties`。
- 生产服务使用 `spring.profiles.active=prod`，并导入对应的 `*-prod.yml` 和 `common-secret-prod.properties`。
- Docker 部署时，服务通过 `NACOS_HOST`、`NACOS_PORT`、`NACOS_USERNAME` 和 `NACOS_PASSWORD` 连接 Nacos。
- 直接运行 JAR 时，请通过 Spring 配置或环境变量设置 Nacos Config/Discovery 地址，避免误连 `localhost:8848`。
- Ollama、DashScope、COS/MinIO、SMTP 和 GitHub OAuth 的地址/凭证应按部署环境填写，不要把生产值复制进示例文件。

## 相关文档

- [algorithm-cloud 后端总览](../README.md)
- [Docker 基础设施](../docker-compose-env.yml)
- [SQL 初始化](../sql/README.md)

本目录中的配置模板遵循项目的 [Apache License 2.0](../LICENSE)。
