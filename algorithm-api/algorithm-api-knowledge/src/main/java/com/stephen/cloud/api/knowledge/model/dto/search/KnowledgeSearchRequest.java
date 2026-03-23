package com.stephen.cloud.api.knowledge.model.dto.search;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import java.io.Serializable;

/**
 * 知识库检索请求 DTO：定义 LLM 调用工具时需要填充的参数。
 *
 * @author StephenQiu30
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonClassDescription("算法知识库检索请求")
public class KnowledgeSearchRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 检索关键词或自然语言问题
     */
    @JsonProperty(required = true)
    @JsonPropertyDescription("需要检索的问题或核心算法关键词")
    private String query;

    /**
     * 目标知识库 ID，由 RAG 服务层通过 UserPrompt 注入
     */
    @JsonProperty(required = true)
    @JsonPropertyDescription("当前对话关联的知识库 ID")
    private Long knowledgeBaseId;
}

