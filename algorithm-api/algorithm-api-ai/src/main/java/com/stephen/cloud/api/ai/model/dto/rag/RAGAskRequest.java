package com.stephen.cloud.api.ai.model.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Map;
import jakarta.validation.constraints.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * RAG问答请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "RAG问答请求")
public class RAGAskRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "问题")
    @NotBlank @Size(max = 2000)
    private String question;

    @Schema(description = "知识库ID")
    @NotNull @Positive
    private Long knowledgeBaseId;

    @Schema(description = "检索数量")
    @Min(1) @Max(20)
    private Integer topK = 5;

    @Schema(description = "会话ID（用于多轮对话记忆）")
    @Size(max = 256)
    private String conversationId;

    @Schema(description = "知识库不足时是否允许联网搜索兜底")
    private Boolean enableWebSearchFallback = Boolean.TRUE;
    @Schema(description = "课堂页面状态（算法、数组、当前步骤、计数、辅助数组）；作为数据而非指令")
    private Map<String, Object> teachingContext;
}
