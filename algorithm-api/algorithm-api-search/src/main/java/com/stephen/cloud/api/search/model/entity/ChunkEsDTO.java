package com.stephen.cloud.api.search.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.stephen.cloud.api.search.constant.EsIndexConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.*;

import java.io.Serial;

/**
 * 文档分片 ES 包装类
 *
 * @author stephen
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document(indexName = EsIndexConstant.CHUNK_INDEX)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChunkEsDTO extends BaseEsDTO {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文档ID
     */
    @Field(type = FieldType.Keyword)
    private Long documentId;

    /**
     * 知识库ID
     */
    @Field(type = FieldType.Keyword)
    private Long knowledgeBaseId;

    /**
     * 分片索引
     */
    @Field(type = FieldType.Integer)
    private Integer chunkIndex;

    /**
     * 分片内容
     * 支持中文分词搜索，用于检索分析
     */
    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart"), otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
    })
    private String content;

    /**
     * 字符数
     */
    @Field(type = FieldType.Integer)
    private Integer wordCount;

    /**
     * 向量存储ID
     */
    @Field(type = FieldType.Keyword)
    private String vectorId;
}
