package com.stephen.cloud.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@TableName(value = "knowledge_document")
@Data
public class KnowledgeDocument implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long knowledgeBaseId;

    private Long userId;

    private String originalName;

    private String storagePath;

    private String mimeType;

    private Long sizeBytes;

    private Integer parseStatus;

    private String errorMsg;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}
