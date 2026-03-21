package com.stephen.cloud.common.auth.config.condition;

import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * SaToken 认证鉴权自定义配置条件
 *
 * @author StephenQiu30
 */
public class SaCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, @NotNull AnnotatedTypeMetadata metadata) {
        return context.getEnvironment().getProperty("sa-token.enable-sa", Boolean.class, false);
    }

}