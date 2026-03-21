package com.stephen.cloud.mail.service.impl;

import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.mail.model.enums.EmailTemplate;
import com.stephen.cloud.mail.service.EmailTemplateService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * 邮件模板服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class EmailTemplateServiceImpl implements EmailTemplateService {

    @Resource
    private TemplateEngine templateEngine;

    @Override
    public String renderTemplate(EmailTemplate template, Map<String, Object> variables) {
        // 通过业务枚举获取模板路径，保证模板引用的一致性
        return renderTemplate(template.getTemplatePath(), variables);
    }

    @Override
    public String renderTemplate(String templateName, Map<String, Object> variables) {
        try {
            // Thymeleaf 上下文对象，用于存放模板变量
            Context context = new Context();
            if (variables != null && !variables.isEmpty()) {
                context.setVariables(variables);
            }

            // 执行模板引擎渲染流程，将变量填充至 HTML 模板中
            String renderedContent = templateEngine.process(templateName, context);
            log.debug("邮件模板渲染成功: {}", templateName);
            return renderedContent;
        } catch (Exception e) {
            log.error("邮件模板渲染失败: {}, 错误原因: {}", templateName, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "邮件模板渲染失败: " + templateName);
        }
    }
}
