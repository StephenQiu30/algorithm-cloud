package com.stephen.cloud.api.knowledge.model.dto.knowledgedocument;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 编辑知识文档请求 (用户)
 */
@Data
@Schema(description = "编辑知识文档请求")
public class KnowledgeDocumentEditRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID")
    private Long id;

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名")
    private String originalName;
}
