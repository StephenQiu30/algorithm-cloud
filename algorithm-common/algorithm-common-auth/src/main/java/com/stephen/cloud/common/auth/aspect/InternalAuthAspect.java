package com.stephen.cloud.common.auth.aspect;

import cn.hutool.core.util.StrUtil;
import com.stephen.cloud.common.auth.annotation.InternalAuth;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.constants.SecurityConstant;
import com.stephen.cloud.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 内部调用认证切面
 * 检查被 @InternalAuth 注解标识的方法，验证其请求头中是否包含正确的内部调用标识
 *
 * @author StephenQiu30
 */
@Aspect
@Component
@Slf4j
public class InternalAuthAspect {

    /**
     * 环绕通知，执行内部调用校验
     *
     * @param point        切入点
     * @param internalAuth 内部调用认证注解
     * @return 原方法的执行结果
     * @throws Throwable 原方法可能抛出的异常或由于无权访问抛出的 BusinessException
     */
    @Around("@annotation(internalAuth)")
    public Object around(ProceedingJoinPoint point, InternalAuth internalAuth) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无法获取请求上下文");
        }

        HttpServletRequest request = attributes.getRequest();
        String fromSource = request.getHeader(SecurityConstant.FROM_SOURCE);

        // 校验内部调用标识
        if (!StrUtil.equals(fromSource, SecurityConstant.INNER)) {
            log.warn("拦截到非内部调用访问 @InternalAuth 接口: {}", request.getRequestURI());
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅限内部调用");
        }

        return point.proceed();
    }
}
