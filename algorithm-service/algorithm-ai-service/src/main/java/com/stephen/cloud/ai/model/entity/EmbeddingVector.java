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
 * 向量元数据实体：记录分片的向量化过程（模型、维度、ES 关联 id）。
 *
 * @author StephenQiu30
 */
@TableName("embedding_vector")
@Data
public class EmbeddingVector implements Serializable {

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的文档分片 ID
     */
    private Long chunkId;

    /**
     * 使用的向量化模型名称
     */
    private String embeddingModel;

    /**
     * 向量维度
     */
    private Integer dimension;

    /**
     * Elasticsearch 中的文档 ID (通常与 chunkId 一致)
     */
    private String esDocId;

    /**
     * 创建时间
     */
    private Date createTime;
}
