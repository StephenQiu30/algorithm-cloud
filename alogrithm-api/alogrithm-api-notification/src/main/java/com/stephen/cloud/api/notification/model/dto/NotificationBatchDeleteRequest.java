package com.stephen.cloud.api.notification.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量删除通知请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "批量删除通知请求")
public class NotificationBatchDeleteRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "通知ID列表")
    @NotEmpty
    private List<Long> ids;
}
