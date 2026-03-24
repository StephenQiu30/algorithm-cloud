package com.stephen.cloud.api.ai.model.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "召回率分析请求")
public class RecallAnalysisRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "问题/查询内容")
    private String question;

    @Schema(description = "知识库ID")
    private Long knowledgeBaseId;

    @Schema(description = "最大召回数量")
    private Integer topK = 10;

    @Schema(description = "相似度阈值")
    private Double similarityThreshold;

    @Schema(description = "是否启用重排")
    private Boolean enableRerank = true;
}
