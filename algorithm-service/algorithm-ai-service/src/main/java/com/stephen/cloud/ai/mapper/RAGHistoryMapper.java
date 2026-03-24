package com.stephen.cloud.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stephen.cloud.ai.model.entity.RAGHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * RAG问答历史 Mapper
 *
 * @author StephenQiu30
 * @Entity com.stephen.cloud.ai.model.entity.RAGHistory
 */
@Mapper
public interface RAGHistoryMapper extends BaseMapper<RAGHistory> {

}
