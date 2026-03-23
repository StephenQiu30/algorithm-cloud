package com.stephen.cloud.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 向量元数据实体：记录分片的向量化过程（模型、维度、ES 关联 id）。
 *
 * @author StephenQiu30
 */
@TableName("embedding_vector")
@Data
public class EmbeddingVector implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分片ID
     */
    private Long chunkId;

    /**
     * 模型名
     */
    private String embeddingModel;

    /**
     * 维度
     */
    private Integer dimension;

    /**
     * ES文档ID
     */
    private String esDocId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
