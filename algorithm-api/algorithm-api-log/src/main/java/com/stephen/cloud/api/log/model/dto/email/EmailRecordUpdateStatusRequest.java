package com.stephen.cloud.api.log.model.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "邮件记录状态更新请求")
public class EmailRecordUpdateStatusRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "发送状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "错误信息")
    private String errorMessage;
}
