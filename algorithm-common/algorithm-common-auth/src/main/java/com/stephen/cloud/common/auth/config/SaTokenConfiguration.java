package com.stephen.cloud.common.auth.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.stephen.cloud.common.auth.config.condition.SaCondition;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.constants.SecurityConstant;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * SaToken 认证配置
 *
 * @author StephenQiu30
 */
@Configuration
@Slf4j
@Conditional(SaCondition.class)
public class SaTokenConfiguration implements WebMvcConfigurer {

    /**
     * 定义 SaToken 不需要拦截的URI
     */
    private static final List<String> SA_TOKEN_NOT_NEED_INTERCEPT_URI = new ArrayList<>() {
        @Serial
        private static final long serialVersionUID = 5839574116900754104L;

        {
            add("/");
            add("/user/register");
            add("/user/login");
            add("/user/login/**");
            add("/user/wx/login");
            add("/user/login/wx/qrcode");
            add("/user/login/wx/status");
            add("/mail/email/code/**");
            add("/swagger-ui.html");
            add("/webjars/**");
            add("/swagger-ui/**");
            add("/v3/api-docs/**");
            add("/doc.html");
        }
    };

    /**
     * 注册sa-token的拦截器
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // 注册路由拦截器，自定义验证规则
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // 校验上下文是否初始化
                    if (!SaManager.getSaTokenContext().isValid()) {
                        return;
                    }
                    // 自定义放行逻辑：如果请求头中包含内部调用标识，则放行
                    String fromSource = SaHolder.getRequest().getHeader(SecurityConstant.FROM_SOURCE);
                    if (StrUtil.equals(fromSource, SecurityConstant.INNER)) {
                        SaRouter.stop();
                    }
                    // 默认校验登录状态（如果需要更细粒度的校验，可以使用注解或在此处继续添加逻辑）
                }))
                .addPathPatterns("/**")
                .excludePathPatterns(SA_TOKEN_NOT_NEED_INTERCEPT_URI);
    }

    /**
     * 静态资源映射
     *
     * @param registry 静态资源注册器
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 映射 Swagger静态资源
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/swagger-ui/");
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("classpath:/resources/");
    }

    /**
     * 注册 [Sa-Token全局过滤器]
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
                // 异常处理函数：每次认证函数发生异常时执行此函数
                .setError(e -> {
                    // 设置响应头
                    SaHolder.getResponse().setHeader("Content-Type", "application/json;charset=UTF-8");
                    // 使用封装的 JSON 工具类转换数据格式，遵循项目统标一
                    return JSONUtil.toJsonStr(ResultUtils.error(ErrorCode.OPERATION_ERROR, e.getMessage()));
                })
                // 前置函数：在每次认证函数之前执行（BeforeAuth 不受 includeList 与 excludeList 的限制，所有请求都会进入）
                .setBeforeAuth(r -> {
                    // ---------- 设置一些安全响应头 ----------
                    SaHolder.getResponse()
                            // 服务器名称
                            .setServer("sa-server")
                            // 是否可以在iframe显示视图： DENY=不可以 | SAMEORIGIN=同域下可以 | ALLOW-FROM uri=指定域名下可以
                            .setHeader("X-Frame-Options", "SAMEORIGIN")
                            // 是否启用浏览器默认XSS防护： 0=禁用 | 1=启用 | 1; mode=block 启用, 并在检查到XSS攻击时，停止渲染页面
                            .setHeader("X-XSS-Protection", "1; mode=block")
                            // 禁用浏览器内容嗅探
                            .setHeader("X-Content-Type-Options", "nosniff");
                });

    }

    /**
     * 依赖注入日志输出
     */
    @PostConstruct
    private void initDi() {
        log.info("############ {} Configuration DI.", this.getClass().getSimpleName());
    }

}
