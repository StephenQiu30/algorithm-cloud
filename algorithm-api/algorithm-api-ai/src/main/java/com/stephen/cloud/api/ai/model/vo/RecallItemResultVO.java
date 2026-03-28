package com.stephen.cloud.api.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 单项召回结果视图对象
 * 用于展示单个问题的召回测试结果
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "单项召回结果")
public class RecallItemResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "问题")
    private String question;

    @Schema(description = "是否命中（至少命中一个期望分片）")
    private Boolean isHit;

    @Schema(description = "召回率（期望分片被找回的比例）")
    private Double recall;

    @Schema(description = "准确率 (Precision)")
    private Double precision;

    @Schema(description = "平均倒数排名 (MRR)")
    private Double mrr;

    @Schema(description = "实际召回的分片内容列表")
    private List<RetrievalHitVO> retrievedChunks;

    @Schema(description = "平均相似度")
    private Double avgSimilarity;

    @Schema(description = "最高相似度")
    private Double maxSimilarity;
}
