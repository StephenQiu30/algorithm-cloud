package com.stephen.cloud.api.user.model.dto;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户查询请求
 *
 * @author StephenQiu30
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "用户查询请求")
public class UserQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     * 精确查询用户ID
     */
    @Schema(description = "用户ID")
    private Long id;

    /**
     * 排除的用户ID
     * 查询结果中不包含该用户
     */
    @Schema(description = "排除的用户ID")
    private Long notId;

    /**
     * 用户昵称
     * 支持模糊查询
     */
    @Schema(description = "用户昵称")
    private String userName;

    /**
     * 用户角色
     * user-普通用户, admin-管理员, ban-封禁用户
     */
    @Schema(description = "用户角色")
    private String userRole;

    /**
     * 用户邮箱
     * 支持模糊查询
     */
    @Schema(description = "用户邮箱")
    private String userEmail;

    /**
     * 用户电话
     * 支持模糊查询
     */
    @Schema(description = "用户电话")
    private String userPhone;

    /**
     * 搜索文本
     * 在昵称、简介等字段中搜索
     */
    @Schema(description = "搜索文本")
    private String searchText;

    /**
     * 排序字段
     * 如createTime, updateTime等
     */
    @Schema(description = "排序字段")
    private String sortField;

    /**
     * 排序方式
     * asc-升序, desc-降序
     */
    @Schema(description = "排序方式")
    private String sortOrder;
}
