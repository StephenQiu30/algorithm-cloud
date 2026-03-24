package com.stephen.cloud.api.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "批量召回分析结果")
public class BatchRecallVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "总命中率 (Hit Rate)")
    private Double overallHitRate;

    @Schema(description = "平均召回率 (Mean Recall)")
    private Double meanRecall;

    @Schema(description = "测试结果详情")
    private List<RecallItemResultVO> itemResults;
}
