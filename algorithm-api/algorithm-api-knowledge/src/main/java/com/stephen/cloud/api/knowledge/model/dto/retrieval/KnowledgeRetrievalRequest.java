package com.stephen.cloud.api.knowledge.model.dto.retrieval;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识检索请求
 * <p>
 * 用于对特定知识库进行语义检索，返回相似度评分和文本切片。
 * </p>
 */
/**
 * 知识检索请求
 * <p>
 * 用于对特定知识库进行语义检索，返回相似度评分和文本切片。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "知识检索请求")
public class KnowledgeRetrievalRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 知识库 ID
     */
    @Schema(description = "知识库 ID")
    private Long knowledgeBaseId;

    /**
     * 检索关键字/提问
     */
    @Schema(description = "查询内容")
    private String query;

    /**
     * 检索数量
     */
    @Schema(description = "检索数量", example = "5")
    private Integer topK;
}

