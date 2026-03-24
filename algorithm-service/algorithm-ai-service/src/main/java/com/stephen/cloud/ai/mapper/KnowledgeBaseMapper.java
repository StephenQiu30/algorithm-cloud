package com.stephen.cloud.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库 Mapper
 *
 * @author StephenQiu30
 * @Entity com.stephen.cloud.ai.model.entity.KnowledgeBase
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

}
