package com.stephen.cloud.api.ai.model.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "批量召回率分析请求")
public class BatchRecallRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "检索配置")
    private RecallAnalysisRequest config;

    @Schema(description = "测试项列表")
    private List<RecallTestItem> items;
}
