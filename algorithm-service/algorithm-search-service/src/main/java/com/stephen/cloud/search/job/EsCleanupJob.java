package com.stephen.cloud.search.job;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import com.stephen.cloud.api.search.constant.EsIndexConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ES 数据清理定时任务
 * <p>
 * 定期清理 Elasticsearch 中已删除的数据
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class EsCleanupJob {

    @Resource
    private ElasticsearchClient elasticsearchClient;

    /**
     * 清理 ES 中的已删除数据
     * 每天凌晨 4 点执行
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupDeletedData() {
        log.info("[EsCleanupJob] 开始清理 ES 中的已删除数据...");

        try {
            long postDeletedCount = cleanupIndexByIsDelete(EsIndexConstant.POST_INDEX, "post");
            long userDeletedCount = cleanupIndexByIsDelete(EsIndexConstant.USER_INDEX, "user");

            log.info("[EsCleanupJob] 清理完成, 帖子删除 {} 条, 用户删除 {} 条", postDeletedCount, userDeletedCount);
        } catch (Exception e) {
            log.error("[EsCleanupJob] 清理 ES 数据失败", e);
        }
    }

    /**
     * 根据索引清理已删除的数据
     *
     * @param index 索引名称
     * @param type  数据类型（用于日志）
     * @return 删除的数量
     */
    private long cleanupIndexByIsDelete(String index, String type) {
        try {
            TermQuery isDeleteQuery = TermQuery.of(t -> t
                    .field("isDelete")
                    .value(1));

            DeleteByQueryRequest request = DeleteByQueryRequest.of(d -> d
                    .index(index)
                    .query(isDeleteQuery._toQuery()));

            DeleteByQueryResponse response = elasticsearchClient.deleteByQuery(request);

            long deletedCount = response.deleted();
            log.info("[EsCleanupJob] {} 索引清理完成，删除 {} 条数据", type, deletedCount);

            return deletedCount;
        } catch (Exception e) {
            log.error("[EsCleanupJob] 清理 {} 索引失败", type, e);
            return 0;
        }
    }
}
