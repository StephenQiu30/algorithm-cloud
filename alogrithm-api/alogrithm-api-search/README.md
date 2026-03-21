# alogrithm-api-search - 搜索服务 API 交互层

本模块提供了搜索服务的标准 RPC 协议，支撑微服务生态下的跨源垂直检索与聚合搜索功能。

## 🌟 核心功能

- **SearchFeignClient**:
    - 提供高吞吐的搜索请求接口。
    - 支持 `SearchRequest` 统一封装，实现对帖子、用户等多维度的弹性检索。

## 🛠️ 接入示例

```java
@Resource
private SearchFeignClient searchFeignClient;

public Page<PostVO> searchFromEs(String keyword) {
    SearchRequest req = new SearchRequest();
    req.setSearchText(keyword);
    req.setType(SearchTypeEnum.POST.getValue());
    return searchFeignClient.searchPostVo(req).getData();
}
```
