package com.stephen.cloud.api.knowledge.model.dto.chunk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新文档分片请求
 */
/**
 * 更新文档分片请求
 * 
 * @author StephenQiu30
 */
@Data
@Schema(description = "更新文档分片请求")
public class DocumentChunkUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分片 ID
     */
    @Schema(description = "主键 ID")
    private Long id;

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
