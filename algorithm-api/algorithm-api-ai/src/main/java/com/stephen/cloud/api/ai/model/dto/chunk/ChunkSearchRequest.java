package com.stephen.cloud.api.ai.model.dto.chunk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "分片内容检索请求")
public class ChunkSearchRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "检索内容")
    private String query;

    @Schema(description = "知识库ID")
    private Long knowledgeBaseId;

    @Schema(description = "文档ID")
    private Long documentId;

    @Schema(description = "返回数量", example = "10")
    private Integer topK;

    @Schema(description = "相似度阈值", example = "0.5")
    private Double similarityThreshold;
}
