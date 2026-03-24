package com.stephen.cloud.api.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "RAG答案视图对象")
public class RAGAnswerVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "答案")
    private String answer;

    @Schema(description = "引用来源")
    private List<SourceVO> sources;

    @Schema(description = "响应时间(毫秒)")
    private Long responseTime;
}
