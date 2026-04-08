package com.stephen.cloud.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮箱验证码配置属性
 * <p>
 * 控制验证码的有效期、长度、发送频率限制等
 * </p>
 *
 * @author StephenQiu30
 */
@Data
@Component
@ConfigurationProperties(prefix = "login.email.code")
public class EmailCodeProperties {

    private int expireTime = 300;
    private int length = 6;
    private int sendLimit = 60;
    private int ipLimit = 10;
}
