package com.stephen.cloud.api.search.model.dto;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询请求
 *
 * @author StephenQiu30
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "搜索请求对象")
public class SearchRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 8341366765860156611L;

    /**
     * 搜索词
     */
    @Schema(description = "搜索词")
    private String searchText;

    /**
     * 分类
     */
    @Schema(description = "分类")
    private String type;
}
