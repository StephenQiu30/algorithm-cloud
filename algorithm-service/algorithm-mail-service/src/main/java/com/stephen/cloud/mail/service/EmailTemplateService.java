package com.stephen.cloud.mail.service;

import com.stephen.cloud.mail.model.enums.EmailTemplate;

import java.util.Map;

/**
 * 邮件模板服务接口
 *
 * @author StephenQiu30
 */
public interface EmailTemplateService {

    /**
     * 渲染邮件模板
     *
     * @param template  模板类型
     * @param variables 模板变量
     * @return 渲染后的 HTML 内容
     */
    String renderTemplate(EmailTemplate template, Map<String, Object> variables);

    /**
     * 渲染自定义邮件模板
     *
     * @param templateName 模板名称（相对于 templates 目录）
     * @param variables    模板变量
     * @return 渲染后的 HTML 内容
     */
    String renderTemplate(String templateName, Map<String, Object> variables);
}
