package com.stephen.cloud.common.common;

import com.stephen.cloud.common.constants.CommonConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "分页请求")
public class PageRequest {

    /**
     * 当前页号
     */
    @Schema(description = "当前页号")
    private int current = 1;

    /**
     * 页面大小
     */
    @Schema(description = "页面大小")
    private int pageSize = 10;

    /**
     * 排序字段
     */
    @Schema(description = "排序字段")
    private String sortField;

    /**
     * 排序顺序（默认升序）
     */
    @Schema(description = "排序顺序（默认升序）")
    private String sortOrder = CommonConstant.SORT_ORDER_ASC;
}
