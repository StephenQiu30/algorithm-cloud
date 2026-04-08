package com.stephen.cloud.search.datasource;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.search.model.dto.SearchRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 数据源接口（需要接入的数据源必须实现）
 * <p>
 * 定义数据源的统一搜索接口，支持多种搜索类型
 * </p>
 *
 * @author StephenQiu30
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
