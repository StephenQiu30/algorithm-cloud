package com.stephen.cloud.api.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 引用来源视图对象
 * 用于展示RAG回答的引用来源信息
 *
 * @author StephenQiu30
 */
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

    @Schema(description = "向量 cosine 相似度（0~1）")
    private Double vectorSimilarity;

    @Schema(description = "关键词相关性分数")
    private Double keywordRelevance;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "业务标签")
    private String bizTag;

    @Schema(description = "命中原因")
    private String matchReason;
}
