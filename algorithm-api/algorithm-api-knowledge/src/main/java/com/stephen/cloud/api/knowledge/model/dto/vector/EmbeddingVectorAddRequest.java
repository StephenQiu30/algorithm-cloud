package com.stephen.cloud.api.knowledge.model.dto.vector;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建向量元数据请求
 */
/**
 * 创建向量元数据请求
 * 
 * @author StephenQiu30
 */
@Data
@Schema(description = "创建向量元数据请求")
public class EmbeddingVectorAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联的文档分片 ID
     */
    @Schema(description = "关联的文档分片 ID")
    private Long chunkId;

    /**
     * 使用的向量化模型名称
     */
    @Schema(description = "使用的向量化模型名称")
    private String embeddingModel;

    /**
     * 向量维度
     */
    @Schema(description = "向量维度")
    private Integer dimension;

    /**
     * Elasticsearch 中的文档 ID
     */
    @Schema(description = "Elasticsearch 中的文档 ID")
    private String esDocId;
}
