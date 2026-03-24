package com.stephen.cloud.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 知识库表
 *
 * @author StephenQiu30
 * @TableName knowledge_base
 */
@TableName(value = "knowledge_base")
@Data
@Schema(description = "知识库表")
public class KnowledgeBase implements Serializable {

    /**
     * 知识库ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "知识库ID")
    private Long id;

    /**
     * 知识库名称
     */
    @Schema(description = "知识库名称")
    private String name;

    /**
     * 知识库描述
     */
    @Schema(description = "知识库描述")
    private String description;

    /**
     * 创建用户ID
     */
    @Schema(description = "创建用户ID")
    private Long userId;

    /**
     * 文档数量
     */
    @Schema(description = "文档数量")
    private Integer documentCount;

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
