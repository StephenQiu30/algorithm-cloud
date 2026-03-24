package com.stephen.cloud.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * RAG问答历史表
 *
 * @author StephenQiu30
 * @TableName rag_history
 */
@TableName(value = "rag_history")
@Data
@Schema(description = "RAG问答历史表")
public class RAGHistory implements Serializable {

    /**
     * 历史记录ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "历史记录ID")
    private Long id;

    /**
     * 知识库ID
     */
    @Schema(description = "知识库ID")
    private Long knowledgeBaseId;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 问题
     */
    @Schema(description = "问题")
    private String question;

    /**
     * 答案
     */
    @Schema(description = "答案")
    private String answer;

    /**
     * 引用来源
     */
    @Schema(description = "引用来源")
    private String sources;

    /**
     * 响应时间
     */
    @Schema(description = "响应时间")
    private Long responseTime;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}
