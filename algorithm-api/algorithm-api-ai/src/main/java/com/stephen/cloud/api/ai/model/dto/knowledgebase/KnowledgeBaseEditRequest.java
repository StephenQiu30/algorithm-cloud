package com.stephen.cloud.api.ai.model.dto.knowledgebase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户编辑知识库请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "用户编辑知识库请求")
public class KnowledgeBaseEditRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "知识库ID")
    private Long id;

    @Schema(description = "知识库名称")
    private String name;

    @Schema(description = "知识库描述")
    private String description;
}
