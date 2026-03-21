package com.stephen.cloud.search.repository;

import com.stephen.cloud.api.search.model.entity.UserEsDTO;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 用户 ES 操作
 *
 * @author stephen
 */
public interface UserEsDao extends ElasticsearchRepository<UserEsDTO, Long> {
}
