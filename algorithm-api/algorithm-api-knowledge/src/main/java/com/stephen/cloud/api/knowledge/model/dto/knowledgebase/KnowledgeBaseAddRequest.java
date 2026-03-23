package com.stephen.cloud.api.knowledge.model.dto.knowledgebase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识库创建请求
 * 
 * @author StephenQiu30
 */
@Data
@Schema(description = "知识库创建请求")
public class KnowledgeBaseAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 知识库名称
     */
    @Schema(description = "知识库名称")
    private String name;

    /**
     * 知识库描述
     */
    @Schema(description = "知识库描述")
    private String description;
}

