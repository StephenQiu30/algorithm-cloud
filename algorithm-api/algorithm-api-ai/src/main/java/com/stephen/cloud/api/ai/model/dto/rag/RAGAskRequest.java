package com.stephen.cloud.api.ai.model.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

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
}
