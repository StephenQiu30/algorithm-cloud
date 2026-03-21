package com.stephen.cloud.api.search.client;

import com.stephen.cloud.api.search.model.SearchRequest;
import com.stephen.cloud.api.search.model.SearchVO;
import com.stephen.cloud.api.search.model.entity.PostEsDTO;
import com.stephen.cloud.api.search.model.entity.UserEsDTO;
import com.stephen.cloud.common.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 搜索服务 Feign 客户端
 *
 * @author stephen
 */
@FeignClient(name = "stephen-search-service", path = "/api/search", contextId = "searchFeignClient")
public interface SearchFeignClient {

    /**
     * 聚合搜索查询
     *
     * @param searchRequest 搜索请求
     * @return 搜索结果
     */
    @PostMapping("/all")
    BaseResponse<SearchVO<Object>> doSearchAll(@RequestBody SearchRequest searchRequest);

    /**
     * 批量同步用户到 ES
     *
     * @param userEsDTOList 用户列表
     * @return 是否成功
     */
    @PostMapping("/user/batch/upsert")
    BaseResponse<Boolean> batchUpsertUser(@RequestBody List<UserEsDTO> userEsDTOList);

    /**
     * 批量同步帖子到 ES
     *
     * @param postEsDTOList 帖子列表
     * @return 是否成功
     */
    @PostMapping("/post/batch/upsert")
    BaseResponse<Boolean> batchUpsertPost(@RequestBody List<PostEsDTO> postEsDTOList);

}
