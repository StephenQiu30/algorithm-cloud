package com.stephen.cloud.api.mail.client;

import com.stephen.cloud.api.mail.model.dto.MailSendCodeRequest;
import com.stephen.cloud.api.mail.model.dto.MailSendRequest;
import com.stephen.cloud.common.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 邮件服务 Feign 客户端
 * <p>
 * 仅负责发邮件，验证码生成/校验/存储由调用方（如 user-service）负责。
 * </p>
 *
 * @author StephenQiu30
 */
@FeignClient(name = "stephen-mail-service", path = "/api/mail", contextId = "mailFeignClient")
public interface MailFeignClient {

    @PostMapping("/send/sync")
    BaseResponse<Boolean> doSendMailSync(@RequestBody MailSendRequest request);

    @PostMapping("/send/async")
    BaseResponse<Boolean> doSendMailAsync(@RequestBody MailSendRequest request);

    @PostMapping("/send/verification-code")
    BaseResponse<Boolean> doSendVerificationCode(@RequestBody MailSendCodeRequest request);
}
