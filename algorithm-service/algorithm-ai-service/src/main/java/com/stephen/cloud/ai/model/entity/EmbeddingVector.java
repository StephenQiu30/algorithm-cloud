package com.stephen.cloud.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@TableName(value = "embedding_vector")
@Data
public class EmbeddingVector implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long chunkId;

    private String embeddingModel;

    private Integer dimension;

    private String esDocId;

    private Date createTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
