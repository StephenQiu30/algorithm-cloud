package com.stephen.cloud.search.annotation;

import com.stephen.cloud.search.model.enums.SearchTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据类型注解
 *
 * @author stephen
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DataSourceType {
    SearchTypeEnum value();
}
