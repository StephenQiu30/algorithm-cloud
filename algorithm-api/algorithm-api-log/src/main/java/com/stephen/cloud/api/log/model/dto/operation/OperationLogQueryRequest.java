package com.stephen.cloud.api.log.model.dto.operation;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 操作日志查询请求
 *
 * @author StephenQiu30
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "操作日志查询请求")
public class OperationLogQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "主键")
    private Long id;

    /**
     * 操作人ID
     */
    @Schema(description = "操作人ID")
    private Long operatorId;

    /**
     * 操作人名称
     */
    @Schema(description = "操作人名称")
    private String operatorName;

    /**
     * 模块
     */
    @Schema(description = "模块")
    private String module;

    /**
     * 操作类型
     */
    @Schema(description = "操作类型")
    private String action;

    /**
     * 是否成功
     */
    @Schema(description = "是否成功")
    private Integer success;

    /**
     * 客户端IP
     */
    @Schema(description = "客户端IP")
    private String clientIp;

    /**
     * 搜索文本
     */
    @Schema(description = "搜索文本")
    private String searchText;
}
