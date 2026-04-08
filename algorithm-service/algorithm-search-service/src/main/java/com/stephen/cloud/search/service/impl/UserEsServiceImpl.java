package com.stephen.cloud.search.service.impl;

import cn.hutool.core.collection.CollUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.search.constant.EsIndexConstant;
import com.stephen.cloud.api.search.model.entity.UserEsDTO;
import com.stephen.cloud.api.user.model.dto.UserQueryRequest;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.search.repository.UserEsDao;
import com.stephen.cloud.search.service.UserEsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户 ES 搜索服务实现
 * <p>
 * 提供用户数据的 Elasticsearch 索引管理和全文检索能力，
 * 支持用户名搜索、角色过滤等功能。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Service
public class UserEsServiceImpl implements UserEsService {

    private static final String DEFAULT_SORT_FIELD = "createTime";

    @Resource
    private ElasticsearchClient elasticsearchClient;

    @Resource
    private UserEsDao userEsDao;

    @Override
    public boolean batchUpsert(List<UserEsDTO> dataList) {
        if (CollUtil.isEmpty(dataList)) {
            return true;
        }
        dataList.forEach(data -> {
            if (data.getIsDelete() == null) {
                data.setIsDelete(0);
            }
        });
        userEsDao.saveAll(dataList);
        return true;
    }

    /**
     * 构建用户的布尔查询构造器
     *
     * @param queryRequest 查询请求对象
     * @return 布尔查询对象
     */
    @Override
    public Query getBoolQueryBuilder(UserQueryRequest queryRequest) {
        Long id = queryRequest.getId();
        Long notId = queryRequest.getNotId();
        String userRole = queryRequest.getUserRole();
        String userEmail = queryRequest.getUserEmail();
        String userName = queryRequest.getUserName();
        String searchText = queryRequest.getSearchText();

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 基础过滤：未删除
        boolBuilder.filter(f -> f.term(t -> t.field("isDelete").value(0)));

        // 精确过滤
        if (id != null) {
            boolBuilder.filter(f -> f.term(t -> t.field("id").value(id)));
        }
        if (notId != null) {
            boolBuilder.mustNot(f -> f.term(t -> t.field("id").value(notId)));
        }
        if (StringUtils.isNotBlank(userRole)) {
            boolBuilder.filter(f -> f.term(t -> t.field("userRole").value(userRole)));
        }
        if (StringUtils.isNotBlank(userEmail)) {
            boolBuilder.filter(f -> f.term(t -> t.field("userEmail").value(userEmail)));
        }

        // 全文检索
        if (StringUtils.isNotBlank(searchText)) {
            boolBuilder.should(s -> s.match(m -> m.field("userName").query(searchText)));
            boolBuilder.minimumShouldMatch("1");
        } else if (StringUtils.isNotBlank(userName)) {
            boolBuilder.should(s -> s.match(m -> m.field("userName").query(userName)));
        }

        return boolBuilder.build()._toQuery();
    }

    /**
     * 构建排序构造器
     *
     * @param sortField 排序字段
     * @param sortOrder 排序顺序
     * @return 排序对象
     */
    @Override
    public SortOptions getSortBuilder(String sortField, String sortOrder) {
        SortOrder order = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder) ? SortOrder.Asc : SortOrder.Desc;
        return new SortOptions.Builder()
                .field(f -> f.field(StringUtils.isNotBlank(sortField) ? sortField : DEFAULT_SORT_FIELD).order(order))
                .build();
    }

    /**
     * 从 Elasticsearch 搜索用户
     *
     * @param queryRequest 查询请求对象
     * @return 分页结果
     */
    @Override
    public Page<UserEsDTO> searchFromEs(UserQueryRequest queryRequest) {
        long current = Math.max(1, queryRequest.getCurrent());
        long pageSize = Math.max(1, queryRequest.getPageSize());
        long from = (current - 1) * pageSize;

        Query query = getBoolQueryBuilder(queryRequest);
        SortOptions sort = getSortBuilder(queryRequest.getSortField(), queryRequest.getSortOrder());

        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(EsIndexConstant.USER_INDEX)
                .query(query)
                .from((int) from)
                .size((int) pageSize)
                .sort(List.of(sort))
                .build();

        log.info("Executing user search request on index: {}, query: {}", EsIndexConstant.USER_INDEX, query.toString());

        try {
            SearchResponse<UserEsDTO> response = elasticsearchClient.search(searchRequest, UserEsDTO.class);
            long total = 0;
            HitsMetadata<UserEsDTO> hits = response.hits();
            if (hits != null && hits.total() != null) {
                total = hits.total().value();
            }
            log.info("User search response: hits={}, status={}", total, response.timedOut() ? "timeout" : "ok");

            List<UserEsDTO> resultList = List.of();
            if (hits != null) {
                List<Hit<UserEsDTO>> hitList = hits.hits();
                if (hitList != null) {
                    resultList = hitList.stream()
                            .map(Hit::source)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                }
            }
            Page<UserEsDTO> page = new Page<>();
            page.setTotal(total);
            page.setRecords(resultList);
            return page;
        } catch (IOException | ElasticsearchException e) {
            if (e instanceof ElasticsearchException esException) {
                log.error("algorithm_user 搜索失败: [{}], 原因: {}", esException.response().error().type(),
                        esException.response().error().reason());
                if (esException.response().error().rootCause() != null) {
                    esException.response().error().rootCause()
                            .forEach(cause -> log.error("Root Cause: [{}], Reason: {}", cause.type(), cause.reason()));
                }
            } else {
                log.error("algorithm_user 搜索失败", e);
            }
            return new Page<>();
        }
    }
}
