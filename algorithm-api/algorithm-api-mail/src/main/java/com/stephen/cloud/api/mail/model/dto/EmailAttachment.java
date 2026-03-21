package com.stephen.cloud.api.mail.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 邮件附件 DTO
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "邮件附件")
public class EmailAttachment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 附件名称
     */
    @NotBlank(message = "附件文件名不能为空")
    @Schema(description = "附件名称")
    private String filename;

    /**
     * 附件内容 (Base64编码)
     */
    @NotBlank(message = "附件内容不能为空")
    @Schema(description = "附件内容(Base64编码)")
    private String content;

    /**
     * 附件大小 (字节)
     */
    @Schema(description = "附件大小(字节)")
    private Long size;

    /**
     * 附件MIME类型
     */
    @Schema(description = "内容类型")
    private String contentType;
}
