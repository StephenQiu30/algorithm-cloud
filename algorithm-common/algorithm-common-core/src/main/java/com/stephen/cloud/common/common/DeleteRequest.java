package com.stephen.cloud.common.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 删除请求
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "删除请求")
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    @Schema(description = "id")
    @NotNull
    private Long id;

    @Serial
    private static final long serialVersionUID = 1L;
}
