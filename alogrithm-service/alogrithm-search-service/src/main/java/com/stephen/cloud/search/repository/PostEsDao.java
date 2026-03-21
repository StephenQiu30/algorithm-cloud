package com.stephen.cloud.search.repository;

import com.stephen.cloud.api.search.model.entity.PostEsDTO;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * 帖子 ES 操作
 *
 * @author stephen
 */
public interface PostEsDao extends ElasticsearchRepository<PostEsDTO, Long> {

    /**
     * 根据用户 ID 查询帖子
     *
     * @param userId 用户 ID
     * @return 帖子列表
     */
    List<PostEsDTO> findByUserId(Long userId);
}
