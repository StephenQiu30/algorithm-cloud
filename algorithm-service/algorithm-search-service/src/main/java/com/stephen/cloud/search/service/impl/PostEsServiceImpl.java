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
import com.stephen.cloud.api.post.model.dto.post.PostQueryRequest;
import com.stephen.cloud.api.search.constant.EsIndexConstant;
import com.stephen.cloud.api.search.model.entity.PostEsDTO;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.search.repository.PostEsDao;
import com.stephen.cloud.search.service.PostEsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 帖子 ES 搜索服务实现
 *
 * @author stephen
 */
@Slf4j
@Service
public class PostEsServiceImpl implements PostEsService {

    private static final String DEFAULT_SORT_FIELD = "createTime";

    @Resource
    private ElasticsearchClient elasticsearchClient;

    @Resource
    private PostEsDao postEsDao;

    @Override
    public boolean batchUpsert(List<PostEsDTO> dataList) {
        if (CollUtil.isEmpty(dataList)) {
            return true;
        }
        dataList.forEach(data -> {
            if (data.getIsDelete() == null) {
                data.setIsDelete(0);
            }
        });
        postEsDao.saveAll(dataList);
        return true;
    }

    /**
     * 构建帖子搜索的布尔查询构造器
     *
     * @param queryRequest 查询请求对象
     * @return 布尔查询对象
     */
    @Override
    public Query getBoolQueryBuilder(PostQueryRequest queryRequest) {
        Long id = queryRequest.getId();
        Long notId = queryRequest.getNotId();
        String searchText = queryRequest.getSearchText();
        String title = queryRequest.getTitle();
        String content = queryRequest.getContent();
        List<String> tags = queryRequest.getTags();
        List<String> orTags = queryRequest.getOrTags();
        Long userId = queryRequest.getUserId();
        Integer reviewStatus = queryRequest.getReviewStatus();

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 基础过滤：未删除
        boolBuilder.filter(f -> f.term(t -> t.field("isDelete").value(0)));

        // 精确过滤
        if (id != null) {
            boolBuilder.filter(f -> f.term(t -> t.field("id").value(id)));
        }
        if (notId != null) {
            boolBuilder.mustNot(m -> m.term(t -> t.field("id").value(notId)));
        }
        if (userId != null) {
            boolBuilder.filter(f -> f.term(t -> t.field("userId").value(userId)));
        }
        if (reviewStatus != null) {
            boolBuilder.filter(f -> f.term(t -> t.field("reviewStatus").value(reviewStatus)));
        }
        // 关键词检索
        if (StringUtils.isNotBlank(searchText)) {
            boolBuilder.should(s -> s.match(m -> m.field("title").query(searchText)));
            boolBuilder.should(s -> s.match(m -> m.field("content").query(searchText)));
            boolBuilder.should(s -> s.match(m -> m.field("reviewMessage").query(searchText)));
            boolBuilder.minimumShouldMatch("1");
        }
        if (StringUtils.isNotBlank(title)) {
            boolBuilder.filter(f -> f.match(m -> m.field("title").query(title)));
        }
        if (StringUtils.isNotBlank(content)) {
            boolBuilder.filter(f -> f.match(m -> m.field("content").query(content)));
        }
        if (StringUtils.isNotBlank(queryRequest.getReviewMessage())) {
            boolBuilder.filter(f -> f.match(m -> m.field("reviewMessage").query(queryRequest.getReviewMessage())));
        }
        if (CollectionUtils.isNotEmpty(tags)) {
            for (String tag : tags) {
                boolBuilder.filter(f -> f.term(t -> t.field("tags").value(tag)));
            }
        }
        if (CollectionUtils.isNotEmpty(orTags)) {
            for (String tag : orTags) {
                boolBuilder.should(s -> s.term(t -> t.field("tags").value(tag)));
            }
            boolBuilder.minimumShouldMatch("1");
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
     * 从 Elasticsearch 搜索帖子
     *
     * @param queryRequest 查询请求对象
     * @return 分页结果
     */
    @Override
    public Page<PostEsDTO> searchFromEs(PostQueryRequest queryRequest) {
        long current = Math.max(1, queryRequest.getCurrent());
        long pageSize = Math.max(1, queryRequest.getPageSize());
        long from = (current - 1) * pageSize;

        Query query = getBoolQueryBuilder(queryRequest);
        SortOptions sort = getSortBuilder(queryRequest.getSortField(), queryRequest.getSortOrder());

        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(EsIndexConstant.POST_INDEX)
                .query(query)
                .from((int) from)
                .size((int) pageSize)
                .sort(List.of(sort))
                .build();

        log.info("Executing post search request on index: {}, query: {}", EsIndexConstant.POST_INDEX, query.toString());

        try {
            SearchResponse<PostEsDTO> response = elasticsearchClient.search(searchRequest, PostEsDTO.class);
            long total = 0;
            HitsMetadata<PostEsDTO> hits = response.hits();
            if (hits != null && hits.total() != null) {
                total = hits.total().value();
            }
            log.info("Post search response: hits={}, status={}", total, response.timedOut() ? "timeout" : "ok");

            List<PostEsDTO> resultList = List.of();
            if (hits != null) {
                List<Hit<PostEsDTO>> hitList = hits.hits();
                if (hitList != null) {
                    resultList = hitList.stream()
                            .map(Hit::source)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                }
            }
            Page<PostEsDTO> page = new Page<>();
            page.setTotal(total);
            page.setRecords(resultList);
            return page;
        } catch (IOException | ElasticsearchException e) {
            if (e instanceof ElasticsearchException esException) {
                log.error("algorithm_post 搜索失败: [{}], 原因: {}", esException.response().error().type(),
                        esException.response().error().reason());
                if (esException.response().error().rootCause() != null) {
                    esException.response().error().rootCause()
                            .forEach(cause -> log.error("Root Cause: [{}], Reason: {}", cause.type(), cause.reason()));
                }
            } else {
                log.error("algorithm_post 搜索失败", e);
            }
            return new Page<>();
        }
    }
}
