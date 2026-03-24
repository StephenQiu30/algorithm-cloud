package com.stephen.cloud.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 文档分片实体类：保存经过处理后的文本片段及其元数据。
 *
 * @author StephenQiu30
 */
@TableName(value = "document_chunk")
@Data
public class DocumentChunk implements Serializable {

    /**
     * 分片 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属文档 ID
     */
    private Long documentId;

    /**
     * 所属知识库 ID
     */
    private Long knowledgeBaseId;

    /**
     * 分片序号（从 0 开始）
     */
    private Integer chunkIndex;

    /**
     * 分片正文内容
     */
    private String content;

    /**
     * 分片标签（逗号分隔）：算法名、数据结构、复杂度等
     */
    private String tags;

    /**
     * 扩展元数据（JSON）：关键词、摘要、难度等
     */
    private String metadataJson;

    /**
     * 分片 Token 估算值
     */
    private Integer tokenEstimate;

    /**
     * 字符数
     */
    private Integer charCount;

    /**
     * 是否包含代码块
     */
    private Integer hasCode;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}
