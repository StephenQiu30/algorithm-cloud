package com.stephen.cloud.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stephen.cloud.ai.model.entity.Document;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档 Mapper
 *
 * @author StephenQiu30
 * @Entity com.stephen.cloud.ai.model.entity.Document
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

}
