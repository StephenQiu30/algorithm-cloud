package com.stephen.cloud.api.notification.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量标记已读请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "批量标记已读请求")
public class NotificationBatchReadRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 通知ID列表
     */
    @Schema(description = "通知ID列表")
    @NotEmpty(message = "ID列表不能为空")
    private List<Long> ids;
}
