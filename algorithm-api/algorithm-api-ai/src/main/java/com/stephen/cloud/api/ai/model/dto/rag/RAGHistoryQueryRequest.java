package com.stephen.cloud.api.ai.model.dto.rag;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "RAG历史查询请求")
public class RAGHistoryQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "知识库ID")
    private Long knowledgeBaseId;

    @Schema(description = "用户ID")
    private Long userId;
}
