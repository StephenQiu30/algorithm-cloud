package com.stephen.cloud.api.knowledge.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 知识文档视图
 * <p>
 * 包含文档的基本信息、解析状态及错误提示信息。
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识文档视图")
public class KnowledgeDocumentVO implements Serializable {

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
     * 解析状态 (0-待解析, 1-解析中, 2-解析成功, 3-解析失败等)
     */
    @Schema(description = "解析状态 (0-待解析, 1-解析中, 2-解析成功, 3-解析失败等)")
    private Integer parseStatus;

    @Schema(description = "解析状态文案")
    private String parseStatusText;

    /**
     * 错误信息 (解析失败时的原因)
     */
    @Schema(description = "错误信息")
    private String errorMsg;

    /**
     * 文件大小 (字节)
     */
    @Schema(description = "文件大小 (字节)")
    private Long sizeBytes;

    /**
     * 分片总数
     */
    @Schema(description = "分片总数")
    private Integer chunkCount;

    /**
     * 字符总数
     */
    @Schema(description = "字符总数")
    private Integer totalChars;

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

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;
}
