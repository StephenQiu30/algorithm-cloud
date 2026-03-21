# algorithm-api-file - 文件服务 API 交互层

本模块为微服务系统提供了标准化的远程文件操作入口，支持跨服务的文件上传、校验及存储反馈。

## 🌟 核心组件

- **FileFeignClient**:
    - 提供 `uploadFile` RPC 接口，支持 Multipart 格式。
    - 兼容多种业务逻辑标识 (`biz`)，自动下发校验规则。

## 🛠️ 接入示例

### 1. 声明依赖

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-api-file</artifactId>
</dependency>
```

### 2. 多步上传调用

```java
@Resource
private FileFeignClient fileFeignClient;

public String processAvatar(MultipartFile file) {
    // 调用远程文件服务进行异步存储
    BaseResponse<String> res = fileFeignClient.uploadFile(file, FileBizEnum.USER_AVATAR.getValue());
    return res.getData(); 
}
```
