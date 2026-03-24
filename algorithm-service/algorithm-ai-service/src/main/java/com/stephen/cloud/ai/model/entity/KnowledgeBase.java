package com.stephen.cloud.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 知识库中心实体类：定义知识库元信息及所属权限。
 *
 * @author StephenQiu30
 */
@TableName(value = "knowledge_base")
@Data
public class KnowledgeBase implements Serializable {

    /**
     * 知识库 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建人 ID
     */
    private Long userId;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 文档总数
     */
    private Integer documentCount;

    /**
     * 分片总数
     */
    private Integer chunkCount;

    /**
     * 最后入库时间
     */
    private Date lastIngestTime;

    /**
     * 库状态（0-正常, 1-禁用）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除 (MyBatis Plus 逻辑删除)
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}
