package com.stephen.cloud.api.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "召回分析视图对象")
public class RecallAnalysisVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "查询内容")
    private String question;

    @Schema(description = "向量检索结果")
    private List<RetrievalHitVO> vectorHits;

    @Schema(description = "关键词检索结果")
    private List<RetrievalHitVO> keywordHits;

    @Schema(description = "混合检索融合结果")
    private List<RetrievalHitVO> fusedHits;

    @Schema(description = "最终重排结果")
    private List<RetrievalHitVO> finalResults;

    @Schema(description = "检索耗时（毫秒）")
    private Long costMs;
}
