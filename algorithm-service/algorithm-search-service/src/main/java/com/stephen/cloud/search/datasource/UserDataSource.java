package com.stephen.cloud.search.datasource;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.search.model.SearchRequest;
import com.stephen.cloud.api.search.model.entity.UserEsDTO;
import com.stephen.cloud.api.user.model.dto.UserQueryRequest;
import com.stephen.cloud.search.annotation.DataSourceType;
import com.stephen.cloud.search.model.enums.SearchTypeEnum;
import com.stephen.cloud.search.service.UserEsService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * 用户数据源
 *
 * @author stephen
 */
@DataSourceType(SearchTypeEnum.USER)
@Component
@Slf4j
public class UserDataSource implements DataSource<Object> {

    @Resource
    private UserEsService userEsService;

    /**
     * 从 ES 中搜索用户
     *
     * @param searchRequest 搜索条件
     * @param request       request
     * @return 分页结果
     */
    @Override
    public Page<Object> doSearch(SearchRequest searchRequest, HttpServletRequest request) {
        UserQueryRequest queryRequest = new UserQueryRequest(); // Renamed userQueryRequest to queryRequest
        BeanUtils.copyProperties(searchRequest, queryRequest); // Used queryRequest
        Page<UserEsDTO> page = (Page<UserEsDTO>) userEsService.searchFromEs(queryRequest); // Renamed method, changed
        // type, changed variable
        // name
        Page<Object> resultPage = new Page<>();
        BeanUtils.copyProperties(page, resultPage); // Used 'page' instead of 'userPage'
        return resultPage;
    }
}
