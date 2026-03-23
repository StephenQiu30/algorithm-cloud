package com.stephen.cloud.api.knowledge.model.dto;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识文档查询请求
 *
 * @author StephenQiu30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识文档查询请求")
public class KnowledgeDocumentQueryRequest extends PageRequest implements Serializable {

    /**
     * 知识库 id
     */
    @Schema(description = "知识库 id")
    private Long knowledgeBaseId;

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名")
    private String originalName;

    /**
     * 解析状态
     */
    @Schema(description = "解析状态")
    private Integer parseStatus;

    @Serial
    private static final long serialVersionUID = 1L;
}
