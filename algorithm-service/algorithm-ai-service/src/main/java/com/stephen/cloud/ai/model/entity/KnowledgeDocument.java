package com.stephen.cloud.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 知识文档实体类：保存原始文本文件的元信息及其解析状态。
 *
 * @author StephenQiu30
 */
@TableName(value = "knowledge_document")
@Data
public class KnowledgeDocument implements Serializable {

    /**
     * 文档 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属知识库 ID
     */
    private Long knowledgeBaseId;

    /**
     * 上传者 ID
     */
    private Long userId;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 存储路径 (COS URL)
     */
    private String storagePath;

    /**
     * MIME 类型
     */
    private String mimeType;

    /**
     * 文档标签（逗号分隔）
     */
    private String tags;

    /**
     * 是否包含代码 (0-否, 1-是)
     */
    private Integer hasCode;

    /**
     * 文件大小 (Bytes)
     */
    private Long sizeBytes;

    /**
     * 分片总数
     */
    private Integer chunkCount;

    /**
     * 总字符数
     */
    private Integer totalChars;

    /**
     * 总 token 数估算
     */
    private Integer totalTokens;

    /**
     * 解析状态（0-排队中, 1-切片中, 2-已完成, 3-失败）
     */
    private Integer parseStatus;

    /**
     * 错误信息（解析失败时记录）
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除 (逻辑删除)
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}
