package com.stephen.cloud.api.ai.model.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

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
    private String question;

    @Schema(description = "知识库ID")
    private Long knowledgeBaseId;

    @Schema(description = "检索数量")
    private Integer topK = 5;

    @Schema(description = "会话ID（用于多轮对话记忆）")
    private String conversationId;

    @Schema(description = "知识库不足时是否允许联网搜索兜底")
    private Boolean enableWebSearchFallback = Boolean.TRUE;
}
