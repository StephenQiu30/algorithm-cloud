package com.stephen.cloud.api.ai.model.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 召回测试项
 * 用于批量召回测试的单个测试项
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "个人召回测试项")
public class RecallTestItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "问题")
    private String question;

    @Schema(description = "期望召回的分片ID列表（空则仅用于查看召回结果）")
    private List<String> expectedChunkIds;
}
