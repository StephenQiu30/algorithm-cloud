package com.stephen.cloud.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 文档分片表
 *
 * @author StephenQiu30
 * @TableName document_chunk
 */
@TableName(value = "document_chunk")
@Data
@Schema(description = "文档分片表")
public class DocumentChunk implements Serializable {

    /**
     * 分片ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "分片ID")
    private Long id;

    /**
     * 文档ID
     */
    @Schema(description = "文档ID")
    private Long documentId;

    /**
     * 知识库ID
     */
    @Schema(description = "知识库ID")
    private Long knowledgeBaseId;

    /**
     * 文档名称（冗余字段，加速 ES 同步避免关联查询）
     */
    @Schema(description = "文档名称")
    private String documentName;

    /**
     * 分片索引（从0开始）
     */
    @Schema(description = "分片索引")
    private Integer chunkIndex;

    /**
     * 分片内容
     */
    @Schema(description = "分片内容")
    private String content;

    /**
     * 章节标题（SmartTextSplitter 切分时的当前标题）
     */
    @Schema(description = "章节标题")
    private String sectionTitle;

    /**
     * 章节路径（标题层级路径，如 "介绍 > 安装 > Docker"）
     */
    @Schema(description = "章节路径")
    private String sectionPath;

    /**
     * 字符数
     */
    @Schema(description = "字符数")
    private Integer wordCount;

    /**
     * Token 数量
     */
    @Schema(description = "Token数量")
    private Integer tokenCount;

    /**
     * 向量存储ID
     */
    @Schema(description = "向量存储ID")
    private String vectorId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    @Schema(description = "是否删除")
    private Integer isDelete;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}
