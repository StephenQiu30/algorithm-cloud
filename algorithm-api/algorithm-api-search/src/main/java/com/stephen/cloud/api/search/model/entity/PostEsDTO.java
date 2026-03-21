package com.stephen.cloud.api.search.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.stephen.cloud.api.search.constant.EsIndexConstant;
import com.stephen.cloud.api.user.model.vo.UserVO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.*;

import java.io.Serial;
import java.util.List;

/**
 * 帖子 ES 包装类
 *
 * @author stephen
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document(indexName = EsIndexConstant.POST_INDEX)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostEsDTO extends BaseEsDTO {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 标题
         * 支持中文分词搜索，可模糊查询
         */
        @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart"), otherFields = {
                        @InnerField(suffix = "keyword", type = FieldType.Keyword)
        })
        private String title;

        /**
         * 内容
         * 支持中文分词搜索，可模糊查询
         */
        @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart"), otherFields = {
                        @InnerField(suffix = "keyword", type = FieldType.Keyword)
        })
        private String content;

        /**
         * 封面
         * 不建立索引，仅用于展示
         */
        @Field(type = FieldType.Keyword, index = false)
        private String cover;

        /**
         * 标签列表
         * 用于精确匹配标签筛选
         */
        @Field(type = FieldType.Keyword)
        private List<String> tags;

        /**
         * 点赞数
         * 用于排序和筛选
         */
        @Field(type = FieldType.Integer)
        private Integer thumbNum;

        /**
         * 收藏数
         * 用于排序和筛选
         */
        @Field(type = FieldType.Integer)
        private Integer favourNum;

        /**
         * 创建用户ID
         * 用于精确查询用户发布的帖子
         */
        @Field(type = FieldType.Keyword)
        private Long userId;

        /**
         * 内容类型（0-帖子，1-算法知识库）
         */
        @Field(type = FieldType.Integer)
        private Integer contentType;

        /**
         * 审核状态（0-待审核，1-通过，2-拒绝）
         */
        @Field(type = FieldType.Integer)
        private Integer reviewStatus;

        /**
         * 审核信息
         */
        @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
        private String reviewMessage;

        /**
         * 用户信息
         */
        @Field(type = FieldType.Object)
        private UserVO userVO;
}
