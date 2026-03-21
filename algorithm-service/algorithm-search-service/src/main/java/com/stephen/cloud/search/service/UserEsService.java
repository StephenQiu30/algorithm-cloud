package com.stephen.cloud.search.service;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.search.model.entity.UserEsDTO;
import com.stephen.cloud.api.user.model.dto.UserQueryRequest;

import java.util.List;

/**
 * 用户 ES 搜索服务
 *
 * @author stephen
 */
public interface UserEsService {

    /**
     * 从 ES 搜索
     *
     * @param queryRequest 查询请求
     * @return 分页结果
     */
    Page<UserEsDTO> searchFromEs(UserQueryRequest queryRequest);

    /**
     * 批量更新/插入数据
     *
     * @param dataList 数据列表
     * @return 是否成功
     */
    boolean batchUpsert(List<UserEsDTO> dataList);

    /**
     * 构建排序
     *
     * @param sortField 排序字段
     * @param sortOrder 排序顺序
     * @return 排序选项
     */
    SortOptions getSortBuilder(String sortField, String sortOrder);

    /**
     * 构建查询条件
     *
     * @param queryRequest 查询请求
     * @return 查询条件
     */
    Query getBoolQueryBuilder(UserQueryRequest queryRequest);
}
