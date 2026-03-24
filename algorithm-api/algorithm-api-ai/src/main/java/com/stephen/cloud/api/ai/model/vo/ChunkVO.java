package com.stephen.cloud.api.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Schema(description = "分片视图对象")
public class ChunkVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "分片ID（ES文档ID）")
    private String id;

    @Schema(description = "文档ID")
    private Long documentId;

    @Schema(description = "文档名称")
    private String documentName;

    @Schema(description = "知识库ID")
    private Long knowledgeBaseId;

    @Schema(description = "分片索引")
    private Integer chunkIndex;

    @Schema(description = "分片内容")
    private String content;

    @Schema(description = "字符数")
    private Integer wordCount;

    @Schema(description = "检索分数")
    private Double score;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "命中原因")
    private String matchReason;

    @Schema(description = "创建时间")
    private Date createTime;
}
