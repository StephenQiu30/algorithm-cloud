package com.stephen.cloud.api.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

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

    @Schema(description = "实际召回的分片内容列表")
    private List<RetrievalHitVO> retrievedChunks;
}
