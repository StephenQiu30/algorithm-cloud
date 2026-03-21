package com.stephen.cloud.common.auth.config.condition;

import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * SaToken 是否使用Jwt自定义配置条件
 *
 * @author StephenQiu30
 */
public class JwtCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, @NotNull AnnotatedTypeMetadata metadata) {
        return context.getEnvironment().getProperty("sa-token.enable-jwt", Boolean.class, false);
    }

}