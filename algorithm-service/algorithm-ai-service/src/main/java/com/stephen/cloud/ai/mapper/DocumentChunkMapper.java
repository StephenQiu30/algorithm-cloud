package com.stephen.cloud.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档分片 Mapper
 *
 * @author StephenQiu30
 * @Entity com.stephen.cloud.ai.model.entity.DocumentChunk
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

}
