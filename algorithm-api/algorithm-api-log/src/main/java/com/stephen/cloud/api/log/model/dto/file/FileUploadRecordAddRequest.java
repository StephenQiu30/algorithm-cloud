package com.stephen.cloud.api.log.model.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件上传记录创建请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "文件上传记录创建请求")
public class FileUploadRecordAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 上传用户ID
     */
    @Schema(description = "上传用户ID")
    private Long userId;

    /**
     * 业务类型
     */
    @Schema(description = "业务类型")
    private String bizType;

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名")
    private String fileName;

    /**
     * 文件大小
     */
    @Schema(description = "文件大小")
    private Long fileSize;

    /**
     * 文件后缀
     */
    @Schema(description = "文件后缀")
    private String fileSuffix;

    /**
     * 内容类型
     */
    @Schema(description = "内容类型")
    private String contentType;

    /**
     * 存储类型
     */
    @Schema(description = "存储类型")
    private String storageType;

    /**
     * 存储桶
     */
    @Schema(description = "存储桶")
    private String bucket;

    /**
     * 对象键/路径
     */
    @Schema(description = "对象键/路径")
    private String objectKey;

    /**
     * 访问URL
     */
    @Schema(description = "访问URL")
    private String url;

    /**
     * 文件MD5
     */
    @Schema(description = "文件MD5")
    private String md5;

    /**
     * 客户端IP
     */
    @Schema(description = "客户端IP")
    private String clientIp;

    /**
     * 上传状态
     */
    @Schema(description = "上传状态")
    private String status;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMessage;
}
