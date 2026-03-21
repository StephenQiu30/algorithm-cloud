package com.stephen.cloud.api.notification.model.dto;

import com.stephen.cloud.common.common.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询通知请求
 *
 * @author StephenQiu30
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "查询通知请求")
public class NotificationQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 通知ID
     * 精确查询通知ID
     */
    @Schema(description = "通知ID")
    private Long id;

    /**
     * 通知类型
     * system/user/comment/like等
     */
    @Schema(description = "通知类型")
    private String type;

    /**
     * 接收用户ID
     * 查询指定用户的通知
     */
    @Schema(description = "接收用户ID")
    private Long userId;

    /**
     * 是否已读
     * 0-未读，1-已读
     */
    @Schema(description = "是否已读")
    private Integer isRead;

    /**
     * 状态
     * 0-正常，1-停用
     */
    @Schema(description = "状态")
    private Integer status;

    /**
     * 关联对象类型
     * post/comment等
     */
    @Schema(description = "关联对象类型")
    private String relatedType;

    /**
     * 搜索文本
     * 在标题、内容中搜索
     */
    @Schema(description = "搜索文本")
    private String searchText;
}
