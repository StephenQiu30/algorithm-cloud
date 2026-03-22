package com.stephen.cloud.api.knowledge.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识库创建请求
 * <p>
 * 用于封装创建知识库所需的必要信息。
 * </p>
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
