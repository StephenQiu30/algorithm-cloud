package com.stephen.cloud.api.file.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文件上传响应
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件上传响应")
public class FileUploadVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件访问地址
     */
    @Schema(description = "文件访问地址")
    private String url;

    /**
     * 文件名
     */
    @Schema(description = "文件名")
    private String fileName;
}
