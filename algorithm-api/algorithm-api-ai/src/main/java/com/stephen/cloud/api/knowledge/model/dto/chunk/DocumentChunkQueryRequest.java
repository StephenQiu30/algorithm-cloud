package com.stephen.cloud.api.knowledge.model.dto.chunk;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文档分片查询请求
 *
 * @author StephenQiu30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文档分片查询请求")
public class DocumentChunkQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文档 ID
     */
    @Schema(description = "文档 ID")
    private Long documentId;

    /**
     * 知识库 ID
     */
    @Schema(description = "知识库 ID")
    private Long knowledgeBaseId;

    /**
     * 分片序号
     */
    @Schema(description = "分片序号")
    private Integer chunkIndex;

    /**
     * 标签过滤 (支持模糊匹配)
     */
    @Schema(description = "标签过滤")
    private String tags;

    /**
     * 是否包含代码
     */
    @Schema(description = "是否包含代码")
    private Boolean hasCode;
}

