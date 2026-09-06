# algorithm-api-post | 帖子与评论 API 契约

`algorithm-api-post` 是 `algorithm-cloud` 的内容服务接口模块，定义帖子、评论、互动数据和内容元信息的跨服务访问协议。

## 提供能力

- `PostFeignClient`：按 ID 获取帖子详情和状态。
- `PostCommentFeignClient`：访问评论树和评论视图对象。
- `PostVO`、`PostCommentVO` 等共享 DTO/VO。
- 支持搜索索引、通知、AI 摘要等服务复用内容数据。

## Maven 接入

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-api-post</artifactId>
</dependency>
```

## 适用场景

搜索服务构建索引、通知服务处理互动事件、AI 服务生成帖子摘要时，应使用本模块保持服务间数据结构一致。

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [帖子服务](../../algorithm-service/algorithm-post-service/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
