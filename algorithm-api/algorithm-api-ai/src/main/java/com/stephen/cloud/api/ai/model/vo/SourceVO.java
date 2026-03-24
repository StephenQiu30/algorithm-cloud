package com.stephen.cloud.api.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "引用来源")
public class SourceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "文档ID")
    private Long documentId;

    @Schema(description = "文档名称")
    private String documentName;

    @Schema(description = "分片索引")
    private Integer chunkIndex;

    @Schema(description = "分片内容")
    private String content;

    @Schema(description = "相似度得分")
    private Double score;
}
