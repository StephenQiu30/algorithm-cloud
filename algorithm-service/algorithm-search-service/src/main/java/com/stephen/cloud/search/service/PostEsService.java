package com.stephen.cloud.search.service;

/**
 * 帖子 ES 服务
 *
 * @author stephen
 */

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.post.model.dto.post.PostQueryRequest;
import com.stephen.cloud.api.search.model.entity.PostEsDTO;

import java.util.List;

/**
 * 帖子 ES 搜索服务
 *
 * @author stephen
 */
public interface PostEsService {

    /**
     * 从 ES 搜索
     *
     * @param queryRequest 查询请求
     * @return 分页结果
     */
    Page<PostEsDTO> searchFromEs(PostQueryRequest queryRequest);

    /**
     * 批量更新/插入数据
     *
     * @param dataList 数据列表
     * @return 是否成功
     */
    boolean batchUpsert(List<PostEsDTO> dataList);

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
    Query getBoolQueryBuilder(PostQueryRequest queryRequest);
}
