package com.stephen.cloud.api.search.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * ES基础包装类
 * <p>
 * Elasticsearch文档基类
 * 包含所有ES文档共有的基础字段
 * </p>
 *
 * @author stephen
 */
@Data
public class BaseEsDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    protected static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 文档ID
     * Elasticsearch文档的唯一标识
     */
    @Id
    private Long id;

    /**
     * 创建时间
     * 文档创建时间，用于范围查询和排序
     */
    @Field(store = true, type = FieldType.Date)
    private Date createTime;

    /**
     * 更新时间
     * 文档最后更新时间，用于范围查询和排序
     */
    @Field(store = true, type = FieldType.Date)
    private Date updateTime;

    /**
     * 是否删除
     * 0-未删除，1-已删除，用于逻辑删除筛选
     */
    @Field(type = FieldType.Integer)
    private Integer isDelete;
}
