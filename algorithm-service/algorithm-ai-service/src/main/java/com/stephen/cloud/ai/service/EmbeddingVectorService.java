package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.EmbeddingVector;

/**
 * 向量元数据表业务服务：记录每个分片对应的嵌入模型、维度及 ES 文档 id（与 {@link DocumentChunk} 主键对齐），便于运维与对账。
 *
 * @author StephenQiu30
 */
public interface EmbeddingVectorService extends IService<EmbeddingVector> {
}
