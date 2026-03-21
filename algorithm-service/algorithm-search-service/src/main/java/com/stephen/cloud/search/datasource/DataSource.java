package com.stephen.cloud.search.datasource;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.search.model.SearchRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 数据源接口（需要接入的数据源必须实现）
 *
 * @author stephen
 */
public interface DataSource<T> {

    /**
     * 搜索
     *
     * @param searchRequest 搜索条件
     * @param request       request
     * @return 分页结果
     */
    Page<T> doSearch(SearchRequest searchRequest, HttpServletRequest request);
}
