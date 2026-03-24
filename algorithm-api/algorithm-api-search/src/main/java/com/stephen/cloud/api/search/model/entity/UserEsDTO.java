package com.stephen.cloud.api.search.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.stephen.cloud.api.search.constant.EsIndexConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.*;

import java.io.Serial;

/**
 * 用户 Elasticsearch DTO
 *
 * @author stephen
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document(indexName = EsIndexConstant.USER_INDEX)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserEsDTO extends BaseEsDTO {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户昵称
     * 支持中文分词搜索，可模糊查询
     */
    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart"), otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
    })
    private String userName;

    /**
     * 用户头像
     * 不建立索引，仅用于展示
     */
    @Field(type = FieldType.Keyword, index = false)
    private String userAvatar;

    /**
     * 用户简介
     * 支持中文分词搜索，可模糊查询
     */
    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart"), otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
    })
    private String userProfile;

    /**
     * 用户角色
     * user-普通用户, admin-管理员, ban-封禁用户
     */
    @Field(type = FieldType.Keyword)
    private String userRole;

    /**
     * 用户邮箱
     * 用于精确搜索
     */
    @Field(type = FieldType.Keyword)
    private String userEmail;

    /**
     * 用户电话
     * 用于精确搜索
     */
    @Field(type = FieldType.Keyword)
    private String userPhone;
}
