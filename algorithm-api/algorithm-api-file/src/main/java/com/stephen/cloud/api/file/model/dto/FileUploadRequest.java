package com.stephen.cloud.api.file.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件上传请求
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件上传请求")
public class FileUploadRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务类型
     */
    @Schema(description = "业务类型")
    private String biz;
}
