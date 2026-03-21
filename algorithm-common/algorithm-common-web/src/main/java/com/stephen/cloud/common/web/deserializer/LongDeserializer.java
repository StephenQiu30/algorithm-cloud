package com.stephen.cloud.common.web.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;

import java.io.IOException;
import java.io.Serial;

/**
 * Long 类型反序列化器
 * <p>
 * 支持从字符串和数字类型反序列化为 Long，解决前端 JavaScript Long 类型精度丢失问题
 * </p>
 *
 * @author StephenQiu30
 */
@JacksonStdImpl
public class LongDeserializer extends JsonDeserializer<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 提供实例
     */
    public static final LongDeserializer INSTANCE = new LongDeserializer();

    @Override
    public Long deserialize(JsonParser p, DeserializationContext context) throws IOException {
        // 处理 null 值
        if (p.getCurrentToken().isScalarValue() && p.getText() == null) {
            return null;
        }

        // 获取文本值（兼容字符串和数字格式）
        String text = p.getValueAsString();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            throw new IOException("无法将值 '" + text + "' 转换为 Long 类型", e);
        }
    }
}