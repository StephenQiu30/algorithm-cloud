package com.stephen.cloud.api.file.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件上传日志 DTO
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件上传日志 DTO")
public class FileUploadLogDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    @Schema(description = "用户 ID")
    private Long userId;

    /**
     * 业务类型
     */
    @Schema(description = "业务类型")
    private String bizType;

    /**
     * 文件名
     */
    @Schema(description = "文件名")
    private String fileName;

    /**
     * 文件大小
     */
    @Schema(description = "文件大小")
    private Long fileSize;

    /**
     * 内容类型
     */
    @Schema(description = "内容类型")
    private String contentType;

    /**
     * 文件访问 URL
     */
    @Schema(description = "文件访问 URL")
    private String fileUrl;

    /**
     * 存储路径/Key
     */
    @Schema(description = "存储路径/Key")
    private String objectKey;

    /**
     * 客户端 IP
     */
    @Schema(description = "客户端 IP")
    private String clientIp;

    /**
     * 处理状态
     */
    @Schema(description = "处理状态")
    private String status;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMessage;
}
