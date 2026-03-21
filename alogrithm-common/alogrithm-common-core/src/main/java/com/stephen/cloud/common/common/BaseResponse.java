package com.stephen.cloud.common.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用返回类
 * 用于统一封装 REST 接口的返回数据格式
 *
 * @param <T> 返回数据的类型
 * @author StephenQiu30
 */
@Data
@Schema(description = "通用返回类")
public class BaseResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 3801016192261040965L;

    /**
     * 状态码
     */
    @Schema(description = "状态码")
    private int code;

    /**
     * 数据
     */
    @Schema(description = "数据")
    private T data;

    /**
     * 消息
     */
    @Schema(description = "消息")
    private String message;

    /**
     * 无参构造函数
     * 主要用于 Jackson 或 Feign 等框架的反序列化操作
     */
    public BaseResponse() {
    }

    /**
     * 全参构造函数
     *
     * @param code    状态码
     * @param data    返回数据
     * @param message 响应消息
     */
    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    /**
     * 构造函数（默认消息为空）
     *
     * @param code 状态码
     * @param data 返回数据
     */
    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    /**
     * 根据错误码创建响应
     *
     * @param errorCode 错误码枚举
     */
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
