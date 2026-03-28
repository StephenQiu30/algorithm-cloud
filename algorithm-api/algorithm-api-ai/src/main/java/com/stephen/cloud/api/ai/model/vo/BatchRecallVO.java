package com.stephen.cloud.api.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量召回分析结果视图对象
 * 用于展示批量召回测试的统计结果
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "批量召回分析结果")
public class BatchRecallVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "总命中率 (Hit Rate)")
    private Double overallHitRate;

    @Schema(description = "平均召回率 (Mean Recall)")
    private Double meanRecall;

    @Schema(description = "平均准确率 (Mean Precision)")
    private Double meanPrecision;

    @Schema(description = "平均倒数排名 (Mean MRR)")
    private Double meanMRR;

    @Schema(description = "测试结果详情")
    private List<RecallItemResultVO> itemResults;
}
