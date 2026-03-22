package com.stephen.cloud.search.datasource;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.post.model.dto.post.PostQueryRequest;
import com.stephen.cloud.api.search.model.SearchRequest;
import com.stephen.cloud.api.search.model.entity.PostEsDTO;
import com.stephen.cloud.search.annotation.DataSourceType;
import com.stephen.cloud.search.model.enums.SearchTypeEnum;
import com.stephen.cloud.search.service.PostEsService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * 帖子数据源
 *
 * @author stephen
 */
@DataSourceType(SearchTypeEnum.POST)
@Component
@Slf4j
public class PostDataSource implements DataSource<Object> {

    @Resource
    private PostEsService postEsService;

    /**
     * 从 ES 中搜索帖子
     *
     * @param searchRequest 搜索条件
     * @param request       request
     * @return 分页结果
     */
    @Override
    public Page<Object> doSearch(SearchRequest searchRequest, HttpServletRequest request) {
        PostQueryRequest queryRequest = new PostQueryRequest(); // Renamed postQueryRequest to queryRequest
        BeanUtils.copyProperties(searchRequest, queryRequest); // Used queryRequest
        Page<PostEsDTO> page = (Page<PostEsDTO>) postEsService.searchFromEs(queryRequest); // Renamed method, changed
        // variable name, added cast,
        // used queryRequest
        Page<Object> resultPage = new Page<>();
        BeanUtils.copyProperties(page, resultPage); // Used new variable name 'page'
        return resultPage;
    }
}
