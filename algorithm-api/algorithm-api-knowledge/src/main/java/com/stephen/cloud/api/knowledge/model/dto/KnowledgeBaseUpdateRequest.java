package com.stephen.cloud.api.knowledge.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识库更新请求
 * <p>
 * 供管理员对知识库进行全量信息的更新及状态管理。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "知识库更新请求")
public class KnowledgeBaseUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Schema(description = "id")
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

    /**
     * 状态 (0-启用, 1-禁用等)
     */
    @Schema(description = "状态")
    private Integer status;
}
