package com.stephen.cloud.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.mapper.EmbeddingVectorMapper;
import com.stephen.cloud.ai.model.entity.EmbeddingVector;
import com.stephen.cloud.ai.service.EmbeddingVectorService;
import org.springframework.stereotype.Service;

/**
 * 向量元数据服务实现：分片与 ES 文档 id、嵌入模型维度的持久化记录。
 *
 * @author StephenQiu30
 */
@Service
public class EmbeddingVectorServiceImpl extends ServiceImpl<EmbeddingVectorMapper, EmbeddingVector> implements EmbeddingVectorService {
}
