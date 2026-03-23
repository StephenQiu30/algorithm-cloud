package com.stephen.cloud.api.knowledge.model.dto.knowledgebase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识库编辑请求
 * <p>
 * 供用户对自己创建的知识库进行基础信息的编辑。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "知识库编辑请求")
public class KnowledgeBaseEditRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID")
    private Long id;

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

