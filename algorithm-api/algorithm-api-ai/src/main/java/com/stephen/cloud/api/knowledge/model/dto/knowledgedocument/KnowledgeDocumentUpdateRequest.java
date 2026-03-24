package com.stephen.cloud.api.knowledge.model.dto.knowledgedocument;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新知识文档请求 (管理员)
 */
@Data
@Schema(description = "更新知识文档请求")
public class KnowledgeDocumentUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID")
    private Long id;

    /**
     * 知识库 ID
     */
    @Schema(description = "知识库 ID")
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

    /**
     * 错误信息 (解析失败原因)
     */
    @Schema(description = "错误信息")
    private String errorMsg;

    /**
     * 文档标签 (逗号分隔)
     */
    @Schema(description = "文档标签")
    private String tags;

    /**
     * 是否包含代码
     */
    @Schema(description = "是否包含代码")
    private Boolean hasCode;
}
