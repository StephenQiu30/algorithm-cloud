package com.stephen.cloud.api.knowledge.model.dto;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识库查询请求
 * <p>
 * 支持根据 ID、名称和用户 ID 进行分页查询。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识库查询请求")
public class KnowledgeBaseQueryRequest extends PageRequest implements Serializable {

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
     * 创建用户 id
     */
    @Schema(description = "用户 id")
    private Long userId;
}
