package com.stephen.cloud.api.notification.model.vo;

import com.stephen.cloud.api.user.model.vo.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 通知视图对象（API传输用）
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "通知视图对象")
public class NotificationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 通知ID
     */
    @Schema(description = "通知ID")
    private Long id;

    /**
     * 通知标题
     */
    @Schema(description = "通知标题")
    private String title;

    /**
     * 通知内容
     */
    @Schema(description = "通知内容")
    private String content;

    /**
     * 通知类型
     */
    @Schema(description = "通知类型")
    private String type;

    /**
     * 接收用户ID
     */
    @Schema(description = "接收用户ID")
    private Long userId;

    /**
     * 接收用户信息
     */
    @Schema(description = "接收用户信息")
    private UserVO userVO;

    /**
     * 关联对象ID
     */
    @Schema(description = "关联对象ID")
    private Long relatedId;

    /**
     * 关联对象类型
     */
    @Schema(description = "关联对象类型")
    private String relatedType;

    /**
     * 是否已读
     */
    @Schema(description = "是否已读")
    private Integer isRead;

    /**
     * 状态（0-正常，1-停用）
     */
    @Schema(description = "状态")
    private Integer status;

    /**
     * 跳转链接
     */
    @Schema(description = "跳转链接")
    private String contentUrl;

    /**
     * 业务幂等ID
     */
    @Schema(description = "业务幂等ID")
    private String bizId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Date updateTime;
}
