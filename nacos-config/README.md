# Nacos 配置管理与导入工具

本目录作为 `algorithm-cloud` 的配置中心源，存放全系统的 Nacos 配置文件模板。通过标准化的配置管理，实现微服务环境的快速迁移与统一管理。

## 📁 目录结构

| 文件/目录                      | 说明                                    |
|:---------------------------|:--------------------------------------|
| `common-*.yml`             | 基础组件配置（MySQL, Redis, RabbitMQ, Web 等） |
| `common-secret.properties` | 敏感信息模板（密码、密钥、身份令牌等）                   |
| `*-prod.yml`               | 生产环境专用覆盖配置                            |
| `import-config.sh`         | 基于 Nacos OpenAPI 的自动化导入脚本             |

## 🚀 配置导入流程

### 1. 自动化导入 (推荐)

系统内置了基于 `curl` 的导入脚本，可以快速将本地配置同步至 Nacos 服务器：

```bash
# 1. 赋予执行权限
chmod +x import-config.sh
# 2. 执行导入 (确保 Nacos 已启动)
./import-config.sh
```

> [!TIP]
> 默认导入地址为 `localhost:8848`，如需修改请编辑脚本开头的 `NACOS_ADDR` 变量。

### 2. 手动导入

在 Nacos 控制台：`配置管理` -> `配置列表` -> `更多` -> `导入配置` -> 选择本目录下的相关文件。

## ⚠️ 重要说明

1. **敏感信息**: 导入前请务必根据 `common-secret.properties.example` 创建 `common-secret.properties`
   ，并修改其中的数据库、Redis、Nacos、AI API Key 等敏感信息。
    - **注意**: `common-secret.properties` 已在 `.gitignore` 中忽略，不会被提交。
2. **命名空间**: 脚本默认导入到命名空间 `algorithm-cloud`（与各服务 application(-prod).yml 中
   `spring.cloud.nacos.config.namespace` 一致），首次导入会自动创建该命名空间。
3. **数据分组**: 统一使用 `DEFAULT_GROUP`。

## 生产/服务器部署

- 各服务需使用 **`spring.profiles.active=prod`**（docker-compose 已设 `SPRING_PROFILES_ACTIVE=prod`）。
- **Nacos 连接**：生产必须能连上 Nacos。使用 docker-compose 时通过环境变量 `NACOS_HOST`、`NACOS_PORT` 传入；若直接
  `java -jar` 部署，须设置 `SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR` 与 `SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR`（如
  `服务器IP:8848`），否则会退化为 localhost:8848 导致连不上。
- **common-secret-prod.properties**：生产库/Redis/RabbitMQ/ES 等地址与密码需与服务器环境一致；`prod.ollama.base-url` 若 AI
  服务在 Docker 而 Ollama 在宿主机，请改为 `http://host.docker.internal:11434`（Mac/Win）或宿主机内网 IP。
