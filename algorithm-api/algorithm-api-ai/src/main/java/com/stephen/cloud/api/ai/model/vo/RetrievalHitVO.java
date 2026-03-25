package com.stephen.cloud.api.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "检索命中详情")
public class RetrievalHitVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "分片ID")
    private String id;

    @Schema(description = "文档ID")
    private Long documentId;

    @Schema(description = "文档名称")
    private String documentName;

    @Schema(description = "分片索引")
    private Integer chunkIndex;

    @Schema(description = "分片内容")
    private String content;

    @Schema(description = "向量分数（相似度）")
    private Double vectorScore;

    @Schema(description = "关键词分数（ES Score）")
    private Double keywordScore;

    @Schema(description = "融合分数（RRF/Rerank）")
    private Double fusionScore;

    @Schema(description = "最终评分")
    private Double score;

    @Schema(description = "向量 cosine 相似度（0~1）")
    private Double similarityScore;

    @Schema(description = "重排分数")
    private Double rerankScore;

    @Schema(description = "命中原因")
    private String matchReason;
}
