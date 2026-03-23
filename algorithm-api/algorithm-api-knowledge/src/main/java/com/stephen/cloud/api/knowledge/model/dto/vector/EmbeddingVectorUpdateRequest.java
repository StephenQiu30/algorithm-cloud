package com.stephen.cloud.api.knowledge.model.dto.vector;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新向量元数据请求
 */
/**
 * 更新向量元数据请求
 * 
 * @author StephenQiu30
 */
@Data
@Schema(description = "更新向量元数据请求")
public class EmbeddingVectorUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID")
    private Long id;

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
