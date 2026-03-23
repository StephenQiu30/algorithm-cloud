package com.stephen.cloud.api.knowledge.model.dto.knowledgedocument;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建知识文档请求
 */
@Data
@Schema(description = "创建知识文档请求")
public class KnowledgeDocumentAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * 存储路径
     */
    @Schema(description = "存储路径")
    private String storagePath;

    /**
     * MIME 类型
     */
    @Schema(description = "MIME 类型")
    private String mimeType;

    /**
     * 文件大小 (字节)
     */
    @Schema(description = "文件大小 (字节)")
    private Long sizeBytes;
}
