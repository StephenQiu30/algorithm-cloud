package com.stephen.cloud.api.search.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 搜索结果 VO
 *
 * @param <T> 数据类型
 * @author stephen
 */
@Data
@Schema(description = "搜索结果视图对象")
public class SearchVO<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 9065946273183024389L;

    /**
     * 数据列表
     */
    @Schema(description = "数据列表")
    private List<T> dataList;

    /**
     * 总条数
     */
    @Schema(description = "总条数")
    private Long total;

    /**
     * 当前页
     */
    @Schema(description = "当前页")
    private Long current;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小")
    private Long pageSize;
}
