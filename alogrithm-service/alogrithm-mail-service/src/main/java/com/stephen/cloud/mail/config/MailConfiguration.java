package com.stephen.cloud.mail.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 邮件配置
 *
 * @author StephenQiu30
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class MailConfiguration {

    @PostConstruct
    private void init() {
        log.info("############ {} Configuration DI.", this.getClass().getSimpleName());
    }
}
