# algorithm-file-service | COS 与 MinIO 文件服务

`algorithm-file-service` 是 `algorithm-cloud` 的统一对象存储入口，为头像、帖子图片、教学文档等业务提供文件上传、格式校验、大小校验和存储地址返回。

## 核心能力

- **多存储适配**：支持腾讯云 COS 和开发环境中的 MinIO。
- **文件准入校验**：按业务类型检查文件扩展名、魔数和大小。
- **统一存储路径**：按服务、业务类型、用户和 UUID 组织对象路径。
- **服务间接入**：提供 Feign API 和 HTTP Multipart 上传接口。
- **记录与审计**：与日志服务协同记录文件上传业务信息。

## 主要 API

以下为服务内部路径；通过网关访问时通常增加 `/api` 前缀。

| 能力 | 方法 | 路径 |
| --- | --- | --- |
| 文件上传 | POST | `/file/upload` |

## 技术栈

- Java 21、Spring Boot、Spring Cloud Alibaba、Nacos
- 腾讯云 COS SDK、MinIO Java SDK
- Hutool、Apache Commons FileUpload
- MySQL、Redis（按配置启用）

## 运行

- 默认服务端口：`8085`
- 必需配置：Nacos 和 COS 或 MinIO 存储桶
- 敏感配置：在 `nacos-config/common-secret.properties` 中设置存储凭证

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [文件 API 契约](../../algorithm-api/algorithm-api-file/README.md)
- [Nacos 配置说明](../../nacos-config/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
