# algorithm-api-search | 搜索服务 API 契约

`algorithm-api-search` 是 `algorithm-cloud` 的搜索服务接口模块，使用 Feign 和统一请求模型提供帖子、用户和聚合搜索的跨服务调用能力。

## 提供能力

- `SearchFeignClient`：调用全文检索和聚合搜索接口。
- `SearchRequest`：统一封装关键词、搜索类型、分页和过滤条件。
- 支持帖子、用户等多数据源检索结果的服务间复用。

## Maven 接入

```xml
<dependency>
    <groupId>com.algorithm.cloud</groupId>
    <artifactId>algorithm-api-search</artifactId>
</dependency>
```

## 使用示例

```java
SearchRequest request = new SearchRequest();
request.setSearchText("快速排序");
request.setType(SearchTypeEnum.POST.getValue());

Page<PostVO> page = searchFeignClient.searchPostVo(request).getData();
```

## 相关文档

- [algorithm-cloud 后端总览](../../README.md)
- [搜索服务](../../algorithm-service/algorithm-search-service/README.md)

本模块基于 [Apache License 2.0](../../LICENSE) 开源。
