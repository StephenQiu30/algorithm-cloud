package com.stephen.cloud.api.knowledge.model.dto.vector;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 向量审计查询请求
 * 
 * @author StephenQiu30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "向量元数据查询请求")
public class EmbeddingVectorQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID")
    private Long id;

    /**
     * 分片 ID
     */
    @Schema(description = "分片 ID")
    private Long chunkId;

    /**
     * 向量模型
     */
    @Schema(description = "向量模型")
    private String embeddingModel;

    /**
     * 向量维度
     */
    @Schema(description = "向量维度")
    private Integer dimension;

    /**
     * Elasticsearch 文档 ID
     */
    @Schema(description = "ES 文档 ID")
    private String esDocId;
}

