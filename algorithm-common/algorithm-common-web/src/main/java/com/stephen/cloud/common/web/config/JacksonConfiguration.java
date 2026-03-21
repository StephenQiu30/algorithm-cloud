package com.stephen.cloud.common.web.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.stephen.cloud.common.web.deserializer.LongDeserializer;
import com.stephen.cloud.common.web.serializer.BigNumberSerializer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Spring MVC Json 配置
 *
 * @author StephenQiu30
 */
@JsonComponent
@Slf4j
public class JacksonConfiguration {
    /**
     * 添加 Long 转 json 精度丢失的配置
     *
     * @return Json 自定义处理器
     */
    @Bean("customizerBean")
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            JavaTimeModule javaTimeModule = new JavaTimeModule();
            // 序列化配置：处理 Long/BigInteger 精度丢失及 BigDecimal 转字符串
            javaTimeModule.addSerializer(Long.class, BigNumberSerializer.INSTANCE);
            javaTimeModule.addSerializer(Long.TYPE, BigNumberSerializer.INSTANCE);
            javaTimeModule.addSerializer(BigInteger.class, BigNumberSerializer.INSTANCE);
            javaTimeModule.addSerializer(BigDecimal.class, ToStringSerializer.instance);
            // 反序列化配置：支持字符串和数字转 Long
            javaTimeModule.addDeserializer(Long.class, LongDeserializer.INSTANCE);
            javaTimeModule.addDeserializer(Long.TYPE, LongDeserializer.INSTANCE);
            // 时间格式化
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
            javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
            builder.modules(javaTimeModule);
            @SuppressWarnings("null")
            TimeZone defaultTimeZone = TimeZone.getDefault();
            builder.timeZone(defaultTimeZone);
        };
    }

    /**
     * 依赖注入日志输出
     */
    @PostConstruct
    private void initDi() {
        log.info("############ {} Configuration DI.", this.getClass().getSimpleName());
    }
}
