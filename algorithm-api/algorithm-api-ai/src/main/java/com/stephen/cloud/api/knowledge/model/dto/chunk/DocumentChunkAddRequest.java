package com.stephen.cloud.api.knowledge.model.dto.chunk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建文档分片请求
 */
@Data
@Schema(description = "创建文档分片请求")
public class DocumentChunkAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属文档 ID
     */
    @Schema(description = "所属文档 ID")
    private Long documentId;

    /**
     * 所属知识库 ID
     */
    @Schema(description = "所属知识库 ID")
    private Long knowledgeBaseId;

    /**
     * 分片序号
     */
    @Schema(description = "分片序号")
    private Integer chunkIndex;

    /**
     * 分片正文内容
     */
    @Schema(description = "分片正文内容")
    private String content;

    /**
     * 分片 Token 估算值
     */
    @Schema(description = "分片 Token 估算值")
    private Integer tokenEstimate;
}
