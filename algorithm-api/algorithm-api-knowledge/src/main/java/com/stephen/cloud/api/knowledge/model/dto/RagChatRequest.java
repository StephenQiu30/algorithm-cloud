package com.stephen.cloud.api.knowledge.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * RAG 对话请求
 * <p>
 * 用于基于具体知识库发起检索增强生成 (RAG) 的提问。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "RAG 对话请求")
public class RagChatRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 问题内容
     */
    @Schema(description = "问题内容", example = "什么是 RAG？")
    private String question;

    /**
     * 会话 ID (用于上下文关联)
     */
    @Schema(description = "会话 ID")
    private String sessionId;

    /**
     * 检索 topK (返回相关文档切片的数量)
     */
    @Schema(description = "检索 topK", example = "5")
    private Integer topK;
}
