package com.stephen.cloud.search.manager;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.search.model.dto.SearchRequest;
import com.stephen.cloud.api.search.model.vo.SearchVO;
import com.stephen.cloud.search.datasource.DataSource;
import com.stephen.cloud.search.datasource.DataSourceRegistry;
import com.stephen.cloud.search.model.enums.SearchTypeEnum;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;

/**
 * 搜索门面
 * <p>
 * 提供统一的搜索入口，根据搜索类型分发到不同的数据源执行查询
 * </p>
 *
 * @author StephenQiu30
 */
@Component
@Slf4j
public class SearchFacade {

    @Resource
    private DataSourceRegistry dataSourceRegistry;

    /**
     * 聚合搜索查询
     *
     * @param searchRequest 搜索请求
     * @param request       HTTP 请求
     * @return 搜索结果
     */
    public SearchVO<Object> searchAll(SearchRequest searchRequest, HttpServletRequest request) {
        String type = Optional.ofNullable(searchRequest.getType())
                .orElse(SearchTypeEnum.POST.getValue());
        DataSource<?> dataSource = dataSourceRegistry.getDataSourceByType(type);
        Page<?> page = dataSource.doSearch(searchRequest, request);
        SearchVO<Object> searchVO = new SearchVO<>();
        searchVO.setDataList(new ArrayList<>(page.getRecords()));
        searchVO.setTotal(page.getTotal());
        searchVO.setCurrent(page.getCurrent());
        searchVO.setPageSize(page.getSize());
        return searchVO;
    }
}
