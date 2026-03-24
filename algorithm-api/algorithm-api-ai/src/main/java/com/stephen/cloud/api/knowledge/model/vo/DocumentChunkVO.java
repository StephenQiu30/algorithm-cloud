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
 * 文档分片视图
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档分片视图")
public class DocumentChunkVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID")
    private Long id;

    /**
     * 文档 ID
     */
    @Schema(description = "文档 ID")
    private Long documentId;

    /**
     * 知识库 ID
     */
    @Schema(description = "知识库 ID")
    private Long knowledgeBaseId;

    /**
     * 分片序号
     */
    @Schema(description = "分片序号")
    private Integer chunkIndex;

    /**
     * 分片内容
     */
    @Schema(description = "分片内容")
    private String content;

    /**
     * 标签列表 (逗号分隔)
     */
    @Schema(description = "标签列表")
    private String tags;

    /**
     * 是否包含代码
     */
    @Schema(description = "是否包含代码")
    private Boolean hasCode;

    /**
     * 字符数
     */
    @Schema(description = "字符数")
    private Integer charCount;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;
}
