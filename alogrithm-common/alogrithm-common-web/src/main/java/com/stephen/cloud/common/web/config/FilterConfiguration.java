package com.stephen.cloud.common.web.config;

import com.stephen.cloud.common.web.filter.TraceFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.DispatcherType;

/**
 * 过滤器配置
 *
 * @author StephenQiu30
 */
@Configuration
public class FilterConfiguration {

    @Bean
    public FilterRegistrationBean<TraceFilter> traceFilterRegistration() {
        FilterRegistrationBean<TraceFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceFilter());
        registration.addUrlPatterns("/*");
        registration.setName("traceFilter");
        registration.setOrder(Integer.MIN_VALUE);
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        return registration;
    }
}
