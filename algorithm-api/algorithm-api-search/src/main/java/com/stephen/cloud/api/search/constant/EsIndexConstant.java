package com.stephen.cloud.api.search.constant;

/**
 * ES 索引常量
 *
 * @author stephen
 */
public interface EsIndexConstant {

    /**
     * 帖子索引
     */
    String POST_INDEX = "algorithm_post";

    /**
     * 用户索引
     */
    String USER_INDEX = "algorithm_user";

    /**
     * 文档分片索引
     */
    String CHUNK_INDEX = "document_chunks_search";

}
