# algorithm-api-file | 文件服务 API 契约

`algorithm-api-file` 是 `algorithm-cloud` 的文件服务 API 模块，使用 Spring Cloud OpenFeign 定义跨服务文件上传、业务类型校验和存储结果返回协议。

## 提供能力

- `FileFeignClient`：调用文件服务的标准 Feign 客户端。
- Multipart 文件上传和业务标识 `biz` 传递。
- 统一的文件 URL/存储结果响应模型。
- 为用户头像、帖子图片和教学文档等场景提供跨服务接入入口。

## Maven 接入

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-api-file</artifactId>
</dependency>
```

## 使用示例

```java
@Resource
private FileFeignClient fileFeignClient;

public String uploadAvatar(MultipartFile file) {
    BaseResponse<String> response = fileFeignClient.uploadFile(
        file,
        FileBizEnum.USER_AVATAR.getValue()
    );
    return response.getData();
}
```

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [文件服务](../../algorithm-service/algorithm-file-service/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
